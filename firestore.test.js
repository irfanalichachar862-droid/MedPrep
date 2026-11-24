const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_student_1";
const BOB_UID = "bob_student_2";
const ADMIN_UID = "admin_irfan_1";
const SPOOF_UID = "spoof_attacker_1";
const BOOTSTRAP_ADMIN_EMAIL = "irfanalichachar862@gmail.com";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(offsetMs = 5000) {
  return new Date(Date.now() - offsetMs);
}

function adminContext() {
  return testEnv
    .authenticatedContext(ADMIN_UID, {
      email: BOOTSTRAP_ADMIN_EMAIL,
      email_verified: true,
    })
    .firestore();
}

function validQuestionPayload(authorId, status = "published") {
  const ts = pastDate(10000);
  return {
    authorId,
    subject: "Biology",
    topic: "Cell Biology",
    stem: "Which organelle synthesizes ATP via oxidative phosphorylation?",
    optionA: "Golgi apparatus",
    optionB: "Mitochondria",
    optionC: "Lysosome",
    optionD: "Ribosome",
    correctOption: "B",
    explanation: "Mitochondria house the electron transport chain.",
    youtubeUrl: "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
    difficulty: "easy",
    status,
    createdAt: ts,
    updatedAt: ts,
  };
}

function validPostPayload(authorId, status = "published") {
  const ts = pastDate(10000);
  return {
    authorId,
    title: "MDCAT Biology High-Yield Notes",
    body: "Review cell organelles and ATP synthesis pathways before the test.",
    subject: "Biology",
    topic: "Cell Biology",
    webUrl: "https://medprep.example.com/biology-notes",
    youtubeUrl: "https://www.youtube.com/watch?v=abc123",
    status,
    createdAt: ts,
    updatedAt: ts,
  };
}

function validKeyPayload(keyCode, adminUid) {
  const ts = pastDate(10000);
  return {
    keyCode,
    label: "Batch 2026 Student 1",
    status: "active",
    assignedUserId: "",
    assignedEmail: "",
    boundDeviceId: "",
    createdBy: adminUid,
    createdAt: ts,
    updatedAt: ts,
  };
}

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
  if (testEnv) await testEnv.cleanup();
});

beforeEach(async () => {
  if (testEnv) await testEnv.clearFirestore();
});

test("Unauthenticated users are denied across all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("questions").where("status", "==", "published").get());
  await assertFails(unauthDb.collection("posts").where("status", "==", "published").get());
  await assertFails(unauthDb.collection("access_keys").doc("MED-1234-ABCD").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
});

test("Questions & Posts: Admin-only write, Student read published only, URL validation enforced", async () => {
  const adminDb = adminContext();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Admin creates published + draft question & study post
  await assertSucceeds(adminDb.collection("questions").doc("q1").set(validQuestionPayload(ADMIN_UID, "published")));
  await assertSucceeds(adminDb.collection("questions").doc("q2").set(validQuestionPayload(ADMIN_UID, "draft")));
  await assertSucceeds(adminDb.collection("posts").doc("p1").set(validPostPayload(ADMIN_UID, "published")));
  await assertSucceeds(adminDb.collection("posts").doc("p2").set(validPostPayload(ADMIN_UID, "draft")));

  // Student CAN query published questions & posts
  await assertSucceeds(aliceDb.collection("questions").where("status", "==", "published").get());
  await assertSucceeds(aliceDb.collection("posts").where("status", "==", "published").get());

  // Student CANNOT read drafts, run unfiltered queries, or create/edit/delete questions or posts
  await assertFails(aliceDb.collection("questions").doc("q2").get());
  await assertFails(aliceDb.collection("posts").doc("p2").get());
  await assertFails(aliceDb.collection("questions").get());
  await assertFails(aliceDb.collection("posts").get());
  await assertFails(aliceDb.collection("questions").doc("q_student").set(validQuestionPayload(ALICE_UID, "published")));
  await assertFails(aliceDb.collection("posts").doc("p_student").set(validPostPayload(ALICE_UID, "published")));
  await assertFails(aliceDb.collection("questions").doc("q1").delete());

  // Reject javascript: malicious URLs on posts/questions
  await assertFails(
    adminDb.collection("posts").doc("p_bad").set({
      ...validPostPayload(ADMIN_UID, "published"),
      webUrl: "javascript:alert(1)",
    })
  );
});

test("Access Keys: 1-User 1-Device lock, anti-hijack, admin reset & revoke", async () => {
  const adminDb = adminContext();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Student cannot create access keys
  await assertFails(aliceDb.collection("access_keys").doc("MED-FAKE-9999").set(validKeyPayload("MED-FAKE-9999", ALICE_UID)));

  // Admin creates a special key
  await assertSucceeds(adminDb.collection("access_keys").doc("MED-7X4K-9P2Q").set(validKeyPayload("MED-7X4K-9P2Q", ADMIN_UID)));

  // Student Alice claims the unclaimed active key and locks it to her device
  await assertSucceeds(
    aliceDb.collection("access_keys").doc("MED-7X4K-9P2Q").update({
      assignedUserId: ALICE_UID,
      assignedEmail: "alice@student.edu",
      boundDeviceId: "device_hash_alice_000111222333",
      activatedAt: pastDate(1000),
      updatedAt: pastDate(1000),
    })
  );

  // Alice can list her own assigned key; Bob cannot list all keys or hijack Alice's key
  await assertSucceeds(aliceDb.collection("access_keys").where("assignedUserId", "==", ALICE_UID).get());
  await assertFails(bobDb.collection("access_keys").get());
  await assertFails(
    bobDb.collection("access_keys").doc("MED-7X4K-9P2Q").update({
      assignedUserId: BOB_UID,
      assignedEmail: "bob@student.edu",
      boundDeviceId: "device_hash_bob_999888777666",
      activatedAt: pastDate(500),
      updatedAt: pastDate(500),
    })
  );

  // Even Alice CANNOT swap her own device ID without Admin reset!
  await assertFails(
    aliceDb.collection("access_keys").doc("MED-7X4K-9P2Q").update({
      boundDeviceId: "device_hash_alice_second_phone_2",
      updatedAt: pastDate(200),
    })
  );

  // Admin CAN reset device lock & revoke key
  await assertSucceeds(
    adminDb.collection("access_keys").doc("MED-7X4K-9P2Q").update({
      assignedUserId: "",
      assignedEmail: "",
      boundDeviceId: "",
      status: "revoked",
      updatedAt: pastDate(100),
    })
  );

  // Student CANNOT claim a revoked key
  await assertFails(
    bobDb.collection("access_keys").doc("MED-7X4K-9P2Q").update({
      assignedUserId: BOB_UID,
      assignedEmail: "bob@student.edu",
      boundDeviceId: "device_hash_bob_999888777666",
      activatedAt: pastDate(50),
      updatedAt: pastDate(50),
    })
  );
});

test("Email spoofing cannot gain Admin privileges", async () => {
  const spoofDb = testEnv
    .authenticatedContext(SPOOF_UID, {
      email: BOOTSTRAP_ADMIN_EMAIL,
      email_verified: false,
    })
    .firestore();

  await assertFails(spoofDb.collection("access_keys").get());
  await assertFails(spoofDb.collection("questions").doc("q_spoof").set(validQuestionPayload(SPOOF_UID, "published")));
  await assertFails(spoofDb.collection("posts").doc("p_spoof").set(validPostPayload(SPOOF_UID, "published")));
});

test("Interactive HTML Topic Lessons & Linked MCQs: Admin-only upload, size limit, student read-only", async () => {
  const adminDb = adminContext();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Admin uploads an interactive HTML lesson for Chemistry
  const htmlPost = {
    ...validPostPayload(ADMIN_UID, "published"),
    subject: "Chemistry",
    topic: "Organic Chemistry",
    htmlFileName: "benzene_resonance.html",
    htmlContent: "<!doctype html><html><body><h1>Benzene Resonance</h1><button onclick='alert(1)'>Flip</button></body></html>",
  };
  await assertSucceeds(adminDb.collection("posts").doc("p_html_chem").set(htmlPost));

  // Admin creates an MCQ linked to that interactive HTML post with multiline structural chemistry formula
  const linkedChemQ = {
    ...validQuestionPayload(ADMIN_UID, "published"),
    subject: "Chemistry",
    topic: "Organic Chemistry",
    linkedPostId: "p_html_chem",
    optionA: "   CH3\n    |\nCH3-C-CH3\n    |\n   OH",
  };
  await assertSucceeds(adminDb.collection("questions").doc("q_chem_linked").set(linkedChemQ));

  // Student can read published interactive HTML post & linked MCQ
  await assertSucceeds(aliceDb.collection("posts").doc("p_html_chem").get());
  await assertSucceeds(aliceDb.collection("questions").doc("q_chem_linked").get());

  // Student CANNOT upload or overwrite interactive HTML posts
  await assertFails(
    aliceDb.collection("posts").doc("p_student_html").set({
      ...validPostPayload(ALICE_UID, "published"),
      htmlContent: "<html><body>Injected</body></html>",
      htmlFileName: "evil.html",
    })
  );
  await assertFails(
    aliceDb.collection("posts").doc("p_html_chem").update({
      htmlContent: "<html><body>Tampered</body></html>",
      updatedAt: pastDate(500),
    })
  );

  // Oversized HTML (>200,000 chars) is rejected even for Admin
  await assertFails(
    adminDb.collection("posts").doc("p_huge_html").set({
      ...validPostPayload(ADMIN_UID, "published"),
      htmlContent: "a".repeat(200005),
      htmlFileName: "huge.html",
    })
  );

  // Admin can issue a custom specific student key
  await assertSucceeds(
    adminDb.collection("access_keys").doc("ALI-MDCAT-2026").set(validKeyPayload("ALI-MDCAT-2026", ADMIN_UID))
  );
});

test("User Profile Extensions, Highlights/Notes & Difficult Questions: Strictly Owner-Only", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate(8000);

  // Alice creates her profile with contactEmail, phoneNumber, subscriptionPlan, and scheduleDays
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Khan",
      email: "alice@student.edu",
      contactEmail: "alice.alt@example.com",
      phoneNumber: "+923001234567",
      subscriptionPlan: "advance",
      scheduleDays: 30,
      targetExam: "MDCAT",
      dailyGoal: 30,
      totalAnswered: 0,
      totalCorrect: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Invalid subscription plan is rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      subscriptionPlan: "vip_hacked",
      updatedAt: pastDate(1000),
    })
  );

  // Alice saves a Highlight/Study Note
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("notes").doc("note_1").set({
      title: "Krebs Cycle Key Enzymes",
      content: "Isocitrate dehydrogenase is the rate-limiting enzyme.",
      subject: "Biology",
      questionId: "q1",
      highlightColor: "amber",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Alice saves a Difficult Exam Question
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("difficult_questions").doc("dq_1").set({
      sourceQuestionId: "q1",
      subject: "Chemistry",
      topic: "Organic Chemistry",
      stem: "Which isomer shows optical activity?",
      optionA: "2-butanol",
      optionB: "1-butanol",
      optionC: "2-propanol",
      optionD: "ethanol",
      correctOption: "A",
      explanation: "2-butanol has a chiral carbon attached to 4 different groups.",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Bob CANNOT read or write Alice's profile, notes, or difficult questions
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("notes").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("difficult_questions").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("notes").doc("note_bob").set({
      title: "Injected",
      content: "Not allowed",
      subject: "Biology",
      highlightColor: "teal",
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("Courses: Admin-only create/edit/delete, Student read published only, linked courseId on Posts", async () => {
  const adminDb = adminContext();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const ts = pastDate(8000);

  const coursePayload = (status = "published") => ({
    authorId: ADMIN_UID,
    title: "MDCAT Organic Chemistry Mastery",
    description: "Complete mechanism-by-mechanism course covering hydrocarbons, aldehydes, and spectroscopy.",
    subject: "Chemistry",
    instructor: "Dr. Irfan Ali",
    durationLabel: "6 Weeks",
    level: "advanced",
    syllabusOutline: "Week 1: Alkanes & Alkenes\nWeek 2: Aromatic Rings\nWeek 3: Carbonyls",
    youtubeUrl: "https://www.youtube.com/watch?v=chem123",
    webUrl: "https://medprep.example.com/chem-course",
    status,
    createdAt: ts,
    updatedAt: ts,
  });

  // Unauthenticated denied
  await assertFails(unauthDb.collection("courses").where("status", "==", "published").get());

  // Admin creates published & draft courses
  await assertSucceeds(adminDb.collection("courses").doc("c_chem").set(coursePayload("published")));
  await assertSucceeds(adminDb.collection("courses").doc("c_draft").set(coursePayload("draft")));

  // Admin attaches a StudyPost to that course
  await assertSucceeds(
    adminDb.collection("posts").doc("p_course_1").set({
      ...validPostPayload(ADMIN_UID, "published"),
      subject: "Chemistry",
      courseId: "c_chem",
    })
  );

  // Admin edits course title & duration
  await assertSucceeds(
    adminDb.collection("courses").doc("c_chem").update({
      title: "MDCAT Organic Chemistry Mastery (2026)",
      durationLabel: "8 Weeks",
      updatedAt: pastDate(1000),
    })
  );

  // Student reads published courses, CANNOT read drafts or unfiltered list or modify courses
  await assertSucceeds(aliceDb.collection("courses").where("status", "==", "published").get());
  await assertSucceeds(aliceDb.collection("courses").doc("c_chem").get());
  await assertFails(aliceDb.collection("courses").doc("c_draft").get());
  await assertFails(aliceDb.collection("courses").get());
  await assertFails(aliceDb.collection("courses").doc("c_student").set({ ...coursePayload("published"), authorId: ALICE_UID }));
  await assertFails(aliceDb.collection("courses").doc("c_chem").delete());

  // Invalid level & malicious URL rejected even for Admin
  await assertFails(
    adminDb.collection("courses").doc("c_bad").set({
      ...coursePayload("published"),
      level: "god_mode",
    })
  );
  await assertFails(
    adminDb.collection("courses").doc("c_bad_url").set({
      ...coursePayload("published"),
      webUrl: "javascript:alert(1)",
    })
  );

  // Admin can delete draft course
  await assertSucceeds(adminDb.collection("courses").doc("c_draft").delete());
});



