const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_101";
const BOB_UID = "bob_uid_202";
const CHARLIE_UID = "charlie_uid_303";

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

function futureDate(msOffset = 60000) {
  return new Date(Date.now() + msOffset);
}

test("Unauthenticated users: denied across all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("public_profiles").where("discoverable", "==", true).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("conversations").where("memberUids", "array-contains", ALICE_UID).get());
  await assertFails(unauthDb.collection("posts").where("visibility", "==", "PUBLIC").get());
  await assertFails(unauthDb.collection("stories").where("visibility", "==", "PUBLIC").get());
  await assertFails(unauthDb.collection("communities").where("visibility", "==", "PUBLIC").get());
  await assertFails(unauthDb.collection("calls").where("participantUids", "array-contains", ALICE_UID).get());
});

test("Public profiles, usernames, and private PII isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate();

  // Reserve username and create public + private user docs
  await assertSucceeds(
    aliceDb.collection("usernames").doc("alice_green").set({
      uid: ALICE_UID,
      username: "alice_green",
      createdAt: ts,
    })
  );

  // Duplicate username or spoofing UID fails
  await assertFails(
    bobDb.collection("usernames").doc("alice_green").set({
      uid: BOB_UID,
      username: "alice_green",
      createdAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("public_profiles").doc(ALICE_UID).set({
      uid: ALICE_UID,
      username: "alice_green",
      displayName: "Alice Green",
      bio: "Emerald explorer",
      publicKeyBase64: "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEtest",
      accountVisibility: "PUBLIC",
      onlineStatusVisible: true,
      online: true,
      discoverable: true,
      followersCount: 0,
      followingCount: 0,
      postsCount: 0,
      lastSeenAt: ts,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Private PII doc
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      email: "alice@example.com",
      messagePermission: "EVERYONE",
      lastSeenVisible: true,
      readReceiptsEnabled: true,
      storyVisibility: "PUBLIC",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Bob can query discoverable public profiles, NOT unfiltered, and CANNOT read Alice's private PII
  await assertSucceeds(bobDb.collection("public_profiles").where("discoverable", "==", true).get());
  await assertFails(bobDb.collection("public_profiles").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());

  // Future timestamp tampered profile rejected
  await assertFails(
    bobDb.collection("public_profiles").doc(BOB_UID).set({
      uid: BOB_UID,
      username: "bob_gold",
      displayName: "Bob",
      accountVisibility: "PUBLIC",
      onlineStatusVisible: true,
      online: true,
      discoverable: true,
      followersCount: 0,
      followingCount: 0,
      postsCount: 0,
      createdAt: futureDate(),
      updatedAt: futureDate(),
    })
  );

  // Ghost field rejected
  await assertFails(
    aliceDb.collection("public_profiles").doc(ALICE_UID).update({
      isVerified: true,
      updatedAt: pastDate(500),
    })
  );
});

test("Conversations & End-to-End Encrypted Messages: member isolation & query alignment", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const ts = pastDate(5000);
  const chatId = "dm_alice_bob";

  // Pre-creation existence check mirroring MessagingRepository.createOrOpenDirectChat
  const beforeSnap = await assertSucceeds(aliceDb.collection("conversations").doc(chatId).get());
  if (beforeSnap.exists) {
    throw new Error("Expected DM conversation to not exist before creation");
  }

  await assertSucceeds(
    aliceDb.collection("conversations").doc(chatId).set({
      type: "DIRECT",
      title: "",
      description: "",
      memberUids: [ALICE_UID, BOB_UID],
      adminUids: [ALICE_UID, BOB_UID],
      memberNames: { [ALICE_UID]: "Alice", [BOB_UID]: "Bob" },
      memberKeys: { [ALICE_UID]: "pubA" },
      archivedBy: [],
      mutedBy: [],
      disappearingHours: 0,
      createdBy: ALICE_UID,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Alice sends E2E encrypted message and updates conversation metadata atomically
  const batch = aliceDb.batch();
  batch.set(aliceDb.collection("conversations").doc(chatId).collection("messages").doc("msg_1"), {
    chatId,
    senderId: ALICE_UID,
    senderName: "Alice",
    memberUids: [ALICE_UID, BOB_UID],
    messageType: "TEXT",
    ciphertext: "dGVzdGNpcGhlcnRleHQ=",
    iv: "dGVzdGl2MTIzNDU2",
    encrypted: true,
    senderPublicKey: "pubA",
    forwarded: false,
    edited: false,
    deletedForAll: false,
    reactions: {},
    readBy: [ALICE_UID],
    deliveredTo: [ALICE_UID],
    createdAt: ts,
    updatedAt: ts,
  });
  batch.update(aliceDb.collection("conversations").doc(chatId), {
    lastMessageText: "End-to-end encrypted message",
    lastMessageSenderId: ALICE_UID,
    lastMessageAt: pastDate(1000),
    updatedAt: pastDate(1000),
  });
  await assertSucceeds(batch.commit());

  // Alice updates typing status
  await assertSucceeds(
    aliceDb.collection("conversations").doc(chatId).collection("typing").doc(ALICE_UID).set({
      uid: ALICE_UID,
      displayName: "Alice",
      memberUids: [ALICE_UID, BOB_UID],
      typing: true,
      updatedAt: pastDate(800),
    })
  );

  // Bob can list conversations, messages & typing with memberUids filter, and mark read
  await assertSucceeds(bobDb.collection("conversations").where("memberUids", "array-contains", BOB_UID).get());
  await assertSucceeds(
    bobDb.collection("conversations").doc(chatId).collection("messages").where("memberUids", "array-contains", BOB_UID).get()
  );
  await assertSucceeds(
    bobDb.collection("conversations").doc(chatId).collection("typing").where("memberUids", "array-contains", BOB_UID).get()
  );
  await assertSucceeds(
    bobDb.collection("conversations").doc(chatId).collection("messages").doc("msg_1").update({
      readBy: [ALICE_UID, BOB_UID],
      deliveredTo: [ALICE_UID, BOB_UID],
      reactions: { [BOB_UID]: "💚" },
      updatedAt: pastDate(500),
    })
  );

  // Charlie cannot read existing DM or send messages in Alice-Bob chat
  await assertFails(charlieDb.collection("conversations").doc(chatId).get());
  await assertFails(charlieDb.collection("conversations").get());
  await assertFails(
    charlieDb.collection("conversations").doc(chatId).collection("messages").doc("msg_2").set({
      chatId,
      senderId: CHARLIE_UID,
      senderName: "Charlie",
      memberUids: [ALICE_UID, BOB_UID, CHARLIE_UID],
      messageType: "TEXT",
      ciphertext: "hijack",
      encrypted: false,
      forwarded: false,
      edited: false,
      deletedForAll: false,
      readBy: [CHARLIE_UID],
      deliveredTo: [CHARLIE_UID],
      createdAt: pastDate(500),
      updatedAt: pastDate(500),
    })
  );
});

test("Social Posts, Reactions, Comments, Stories & Communities", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate(4000);

  await assertSucceeds(
    aliceDb.collection("posts").doc("post_1").set({
      authorId: ALICE_UID,
      authorUsername: "alice_green",
      authorName: "Alice Green",
      caption: "Welcome to #WeConnect",
      mediaBase64List: [],
      hashtags: ["weconnect"],
      visibility: "PUBLIC",
      likesCount: 0,
      commentsCount: 0,
      sharesCount: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(bobDb.collection("posts").where("visibility", "==", "PUBLIC").get());
  await assertFails(bobDb.collection("posts").get());

  // Bob reacts & comments
  await assertSucceeds(
    bobDb.collection("posts").doc("post_1").collection("reactions").doc(BOB_UID).set({
      postId: "post_1",
      uid: BOB_UID,
      emoji: "💚",
      createdAt: pastDate(1000),
    })
  );
  await assertSucceeds(
    bobDb.collection("posts").doc("post_1").collection("comments").doc("c_1").set({
      postId: "post_1",
      authorId: BOB_UID,
      authorName: "Bob",
      authorUsername: "bob_gold",
      text: "Great post!",
      createdAt: pastDate(1000),
    })
  );

  // Bob cannot delete Alice's post
  await assertFails(bobDb.collection("posts").doc("post_1").delete());

  // Communities
  await assertSucceeds(
    aliceDb.collection("communities").doc("comm_1").set({
      name: "Emerald Builders",
      description: "Official community",
      kind: "COMMUNITY",
      visibility: "PUBLIC",
      rulesText: "Be respectful.",
      ownerId: ALICE_UID,
      adminUids: [ALICE_UID],
      memberCount: 1,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    bobDb.collection("communities").doc("comm_1").collection("members").doc(BOB_UID).set({
      communityId: "comm_1",
      uid: BOB_UID,
      displayName: "Bob",
      username: "bob_gold",
      role: "MEMBER",
      joinedAt: pastDate(1000),
    })
  );
  // Bob cannot self-assign ADMIN role on join
  await assertFails(
    bobDb.collection("communities").doc("comm_1").collection("members").doc(BOB_UID + "_bad").set({
      communityId: "comm_1",
      uid: BOB_UID,
      displayName: "Bob",
      username: "bob_gold",
      role: "ADMIN",
      joinedAt: pastDate(1000),
    })
  );
});

test("Calls signaling: participant isolation & terminal status lock", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const ts = pastDate(3000);

  await assertSucceeds(
    aliceDb.collection("calls").doc("call_1").set({
      callerId: ALICE_UID,
      callerName: "Alice",
      calleeId: BOB_UID,
      calleeName: "Bob",
      participantUids: [ALICE_UID, BOB_UID],
      callType: "VIDEO",
      status: "RINGING",
      startedAt: ts,
      updatedAt: ts,
    })
  );

  await assertFails(charlieDb.collection("calls").doc("call_1").get());

  await assertSucceeds(
    bobDb.collection("calls").doc("call_1").update({
      status: "CONNECTED",
      updatedAt: pastDate(1500),
    })
  );

  await assertSucceeds(
    aliceDb.collection("calls").doc("call_1").update({
      status: "ENDED",
      endedAt: pastDate(1000),
      updatedAt: pastDate(1000),
    })
  );

  // Terminal state lock: cannot reopen ENDED call
  await assertFails(
    bobDb.collection("calls").doc("call_1").update({
      status: "CONNECTED",
      updatedAt: pastDate(500),
    })
  );
});
