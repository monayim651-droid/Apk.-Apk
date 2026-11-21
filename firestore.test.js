const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const ADMIN_EMAIL = "monayim651@gmail.com";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function pastDate(msOffset = 2000) {
  return new Date(Date.now() - msOffset);
}

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("posts").where("status", "==", "ACTIVE").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("public_profiles").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("posts").doc("p1").set({
      authorId: ALICE_UID,
      authorName: "Alice",
      authorVerified: false,
      sportTag: "FOOTBALL",
      mediaType: "TEXT",
      content: "Test",
      hashtags: [],
      votesA: 0,
      votesB: 0,
      likesCount: 0,
      commentsCount: 0,
      sharesCount: 0,
      status: "ACTIVE",
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
});

test("UserAccount PII isolation and owner write rules", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();
  const ts = pastDate(5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      displayName: "Alice Striker",
      darkMode: true,
      languageCode: "en",
      favTeamIds: ["arsenal"],
      favPlayerIds: [],
      blockedUserIds: [],
      triviaPoints: 0,
      triviaStreak: 0,
      achievements: ["FIRST_SIGN_IN"],
      highContrast: false,
      matchRemindersEnabled: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Bob cannot read Alice's private PII doc
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  // Alice cannot query all users
  await assertFails(aliceDb.collection("users").get());
  // Alice can partially update preferences without modifying createdAt/userId
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      languageCode: "bn",
      darkMode: false,
      updatedAt: pastDate(1000),
    })
  );
  // Future timestamp rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      languageCode: "bn",
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

test("PublicProfile prevents self-verification unless verified admin", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = pastDate(5000);

  // Self-setting verified: true fails
  await assertFails(
    aliceDb.collection("public_profiles").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      handle: "@alice",
      fanBadge: "Captain",
      verified: true,
      followersCount: 0,
      followingCount: 0,
      onlineStatus: "ONLINE",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Self-setting verified: false succeeds
  await assertSucceeds(
    aliceDb.collection("public_profiles").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      handle: "@alice",
      fanBadge: "Rookie",
      verified: false,
      followersCount: 0,
      followingCount: 0,
      onlineStatus: "ONLINE",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Spoofed unverified admin email cannot verify
  const spoofAdminDb = testEnv.authenticatedContext("spoof", { email: ADMIN_EMAIL, email_verified: false }).firestore();
  await assertFails(
    spoofAdminDb.collection("public_profiles").doc(ALICE_UID).update({
      verified: true,
      updatedAt: pastDate(1000),
    })
  );

  // Real verified admin email can verify
  const realAdminDb = testEnv.authenticatedContext("admin_1", { email: ADMIN_EMAIL, email_verified: true }).firestore();
  await assertSucceeds(
    realAdminDb.collection("public_profiles").doc(ALICE_UID).update({
      verified: true,
      updatedAt: pastDate(1000),
    })
  );
});

test("Posts, Comments, Likes, and Query Alignment", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate(5000);

  await assertSucceeds(
    aliceDb.collection("posts").doc("post_1").set({
      authorId: ALICE_UID,
      authorName: "Alice",
      authorVerified: false,
      sportTag: "CRICKET",
      mediaType: "POLL",
      content: "Who wins tonight's T20 clash?",
      hashtags: ["#T20", "#Cricket"],
      pollOptionA: "Bangladesh",
      pollOptionB: "India",
      votesA: 1,
      votesB: 0,
      likesCount: 0,
      commentsCount: 0,
      sharesCount: 0,
      status: "ACTIVE",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Unfiltered posts list fails
  await assertFails(bobDb.collection("posts").get());
  // Filtered ACTIVE posts query succeeds
  await assertSucceeds(bobDb.collection("posts").where("status", "==", "ACTIVE").get());

  // Bob can vote/like/comment
  await assertSucceeds(
    bobDb.collection("posts").doc("post_1").collection("likes").doc(BOB_UID).set({
      userId: BOB_UID,
      createdAt: pastDate(2000),
    })
  );
  await assertSucceeds(
    bobDb.collection("posts").doc("post_1").update({
      likesCount: 1,
      votesB: 1,
      updatedAt: pastDate(1000),
    })
  );
  await assertSucceeds(
    bobDb.collection("posts").doc("post_1").collection("comments").doc("c1").set({
      authorId: BOB_UID,
      authorName: "Bob",
      content: "Going to be a thriller!",
      status: "ACTIVE",
      createdAt: pastDate(1000),
      updatedAt: pastDate(1000),
    })
  );
  await assertSucceeds(
    bobDb.collection("posts").doc("post_1").collection("comments").where("status", "==", "ACTIVE").get()
  );

  // Bob cannot edit Alice's post content
  await assertFails(
    bobDb.collection("posts").doc("post_1").update({
      content: "Hacked post",
      updatedAt: pastDate(500),
    })
  );
});

test("Groups, Private Chats, Reports, Trivia, and AppConfig", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const adminDb = testEnv.authenticatedContext("admin_1", { email: ADMIN_EMAIL, email_verified: true }).firestore();
  const ts = pastDate(5000);

  // Private group created by Alice
  await assertSucceeds(
    aliceDb.collection("groups").doc("grp_priv").set({
      name: "Gunners Inner Circle",
      description: "Private tactical room",
      sportTag: "FOOTBALL",
      groupKind: "TEAM_CLUB",
      privacy: "PRIVATE",
      ownerId: ALICE_UID,
      memberUids: [ALICE_UID],
      memberCount: 1,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Bob cannot read Alice's private group
  await assertFails(bobDb.collection("groups").doc("grp_priv").get());
  // Bob can query PUBLIC groups or his own joined groups
  await assertSucceeds(bobDb.collection("groups").where("privacy", "==", "PUBLIC").get());
  await assertSucceeds(bobDb.collection("groups").where("memberUids", "array-contains", BOB_UID).get());

  // 1:1 Chat between Alice and Bob
  await assertSucceeds(
    aliceDb.collection("chats").doc("chat_ab").set({
      participantUids: [ALICE_UID, BOB_UID],
      participantNames: ["Alice", "Bob"],
      lastMessage: "Match starting!",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(bobDb.collection("chats").where("participantUids", "array-contains", BOB_UID).get());
  await assertSucceeds(
    bobDb.collection("chats").doc("chat_ab").collection("messages").doc("m1").set({
      senderId: BOB_UID,
      senderName: "Bob",
      text: "Ready!",
      participantUids: [ALICE_UID, BOB_UID],
      createdAt: pastDate(1000),
    })
  );

  // AppConfig: non-admin cannot write, admin can write, signed-in can get
  await assertFails(
    aliceDb.collection("app_config").doc("current").set({
      latestVersionCode: 2,
      latestVersionName: "2.0.0",
      mandatoryUpdate: false,
      maintenanceMode: false,
      whatsNewNotes: "Fake update",
      announcementTitle: "Test",
      announcementBody: "Body",
      playStoreUrl: "https://play.google.com",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    adminDb.collection("app_config").doc("current").set({
      latestVersionCode: 2,
      latestVersionName: "2.0.0",
      mandatoryUpdate: false,
      maintenanceMode: false,
      whatsNewNotes: "Live Cricket Ball-by-Ball & Liquid Glass v2",
      announcementTitle: "MONTANU SCORE v2.0",
      announcementBody: "Welcome to the arena.",
      playStoreUrl: "https://play.google.com/store/apps",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(aliceDb.collection("app_config").doc("current").get());

  // Trivia leaderboard query
  await assertSucceeds(
    aliceDb.collection("trivia_scores").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      points: 150,
      correctAnswers: 15,
      streak: 5,
      badge: "Tactician",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(bobDb.collection("trivia_scores").where("points", ">=", 0).get());
});
