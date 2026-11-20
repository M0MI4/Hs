const { initializeTestEnvironment, assertFails, assertSucceeds } = require('@firebase/rules-unit-testing');
const fs = require('fs');
const assert = require('assert');

const PROJECT_ID = 'hsstore-rules-test';
const ADMIN_EMAIL = 'momiadesigner@gmail.com';
const USER_UID = 'user-123';

async function runTests() {
  const testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules: fs.readFileSync('firestore.rules', 'utf8'),
    },
  });

  try {
    const unauthDb = testEnv.unauthenticatedContext().firestore();
    const userDb = testEnv.authenticatedContext(USER_UID, { email: 'user@example.com' }).firestore();
    const adminDb = testEnv.authenticatedContext('admin-1', { email: ADMIN_EMAIL }).firestore();
    const hamaAdminDb = testEnv.authenticatedContext('admin-2', { email: 'hama1cloner2@gmail.com' }).firestore();

    // 1. /feedback: anyone can create, ONLY admins can read/update/delete
    await assertSucceeds(hamaAdminDb.collection('feedback').get());
    await assertSucceeds(
      unauthDb.collection('feedback').add({
        type: 'BROKEN_LINK',
        subject: 'Broken Link: Test App',
        details: '404 on download',
        recipientEmail: ADMIN_EMAIL,
      })
    );
    await assertSucceeds(
      userDb.collection('feedback').add({
        type: 'BUG',
        subject: 'Crash',
        details: 'Stacktrace',
        recipientEmail: ADMIN_EMAIL,
      })
    );
    await assertFails(unauthDb.collection('feedback').doc('sample-fb').get());
    await assertFails(userDb.collection('feedback').doc('sample-fb').get());
    await assertFails(userDb.collection('feedback').doc('sample-fb').update({ status: 'RESOLVED' }));
    await assertFails(userDb.collection('feedback').doc('sample-fb').delete());

    await assertSucceeds(adminDb.collection('feedback').get());
    await assertSucceeds(
      adminDb.collection('feedback').doc('admin-fb').set({
        type: 'GENERAL',
        subject: 'Admin review',
        details: 'OK',
      })
    );
    await assertSucceeds(adminDb.collection('feedback').doc('admin-fb').update({ status: 'RESOLVED' }));
    await assertSucceeds(adminDb.collection('feedback').doc('admin-fb').delete());

    // 2. /config/store_update & /config/feedback_config: public read, admin-only write
    await assertSucceeds(unauthDb.collection('config').doc('store_update').get());
    await assertSucceeds(unauthDb.collection('config').doc('feedback_config').get());
    await assertFails(
      userDb.collection('config').doc('store_update').set({
        latestVersionCode: 99,
        latestVersionName: '9.9.9',
        downloadUrl: 'https://evil.example/app.apk',
      })
    );
    await assertFails(
      userDb.collection('config').doc('feedback_config').set({
        emailWebhookUrl: 'https://evil.example/hook',
      })
    );
    await assertSucceeds(
      adminDb.collection('config').doc('store_update').set({
        latestVersionCode: 2,
        latestVersionName: '1.1',
        downloadUrl: 'https://cdn.example/hsstore.apk',
        changelog: 'Fixes',
        isMandatory: false,
      })
    );
    await assertSucceeds(
      adminDb.collection('config').doc('feedback_config').set({
        emailWebhookUrl: 'https://formspree.io/f/test',
      })
    );

    console.log('All Firestore security rules checks passed.');
  } finally {
    await testEnv.cleanup();
  }
}

runTests().catch((err) => {
  console.error(err);
  process.exit(1);
});
