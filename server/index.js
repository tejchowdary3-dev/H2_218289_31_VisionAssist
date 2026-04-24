/**
 * VisionAssist MVP API — Express.
 * Set OPENAI_API_KEY for POST /ai-report (optional; returns stub if unset).
 */
import express from "express";

const app = express();
app.use(express.json({ limit: "512kb" }));

const memoryTestResults = [];
const memoryAiReports = [];
const testPlan = {
  letters: "CDEHKNOPRSTUVZ",
  line_length: 5,
  correct_threshold: 4,
  max_retries_per_level: 1,
  contrast_rounds: 3,
  snellen_levels: [
    { label: "20/200", font_sp: 34.0 },
    { label: "20/100", font_sp: 30.0 },
    { label: "20/70", font_sp: 26.0 },
    { label: "20/50", font_sp: 22.0 },
    { label: "20/40", font_sp: 19.0 },
    { label: "20/30", font_sp: 16.0 },
    { label: "20/25", font_sp: 14.0 },
    { label: "20/20", font_sp: 12.0 },
    { label: "20/15", font_sp: 10.0 }
  ],
  near_option_scores: [0.35, 0.62, 0.88],
  instructions: {
    right_eye: "Cover your left eye. Read each letter with your right eye.",
    left_eye: "Cover your right eye. Read each letter with your left eye.",
    initial_helper: "Tap letters in order, then Submit. Need 4/5 correct to go smaller.",
    retry_helper: "Retry this size with a new line.",
    switch_eye_helper: "Switch eyes when ready.",
    near_phase: "Near vision (both eyes): pick the smallest line you can read comfortably.",
    astig_phase: "Astigmatism check: in everyday lighting, do radial spokes look equal?",
    astig_helper: "This is subjective screening only-not a diagnosis.",
    contrast_phase: "Contrast: which letter looks clearer?"
  }
};

/** POST /test-result — store screening payload (MVP in-memory). */
app.post("/test-result", (req, res) => {
  const body = req.body;
  if (!body?.user_id) {
    return res.status(400).json({ error: "user_id required" });
  }
  memoryTestResults.push({ ...body, received_at: new Date().toISOString() });
  res.json({ ok: true, id: String(memoryTestResults.length) });
});

/** GET /history/:userId */
app.get("/history/:userId", (req, res) => {
  const uid = req.params.userId;
  const rows = memoryTestResults.filter((r) => r.user_id === uid);
  res.json({ user_id: uid, items: rows });
});

/** GET /clinics?lat=&lng= — mock clinics; swap for Google Places later. */
app.get("/clinics", (req, res) => {
  const lat = parseFloat(req.query.lat ?? "0");
  const lng = parseFloat(req.query.lng ?? "0");
  const clinics = [
    { id: "c1", name: "Demo Eye Clinic", lat: lat + 0.01, lng: lng + 0.01, distance_km: 1.2, availability: "mock" },
    { id: "c2", name: "Vision Center", lat: lat - 0.008, lng: lng + 0.015, distance_km: 2.4, availability: "mock" }
  ];
  res.json({ lat, lng, clinics });
});

/** GET /test-plan — dynamic configuration consumed by Android app at runtime. */
app.get("/test-plan", (_req, res) => {
  res.json(testPlan);
});

/** POST /ai-report — structured LLM call (non-diagnostic system prompt). */
app.post("/ai-report", async (req, res) => {
  const payload = req.body;
  if (!payload?.user_profile) {
    return res.status(400).json({ error: "expected body from sample-ai-report-request.json" });
  }

  const key = process.env.OPENAI_API_KEY;
  if (!key) {
    const stub = buildStubReport(payload);
    memoryAiReports.push(stub);
    return res.json(stub);
  }

  try {
    const OpenAI = (await import("openai")).default;
    const client = new OpenAI({ apiKey: key });
    const completion = await client.chat.completions.create({
      model: "gpt-4o-mini",
      temperature: 0.3,
      messages: [
        {
          role: "system",
          content:
            "You are a vision screening assistant. Never diagnose. Use cautious language: 'may suggest', 'may indicate'. " +
            "Always include that this is not medical advice. Output strict JSON with keys: summary, key_findings (array), " +
            "risk_level (low|medium|high as screening triage), recommendation, disclaimer."
        },
        { role: "user", content: JSON.stringify(payload) }
      ]
    });
    const text = completion.choices[0]?.message?.content ?? "{}";
    const parsed = JSON.parse(text);
    memoryAiReports.push(parsed);
    res.json(parsed);
  } catch (e) {
    res.status(502).json({ error: String(e) });
  }
});

function buildStubReport(payload) {
  return {
    summary:
      "This is a stub AI report (set OPENAI_API_KEY for live LLM). Screening data was received; results are not diagnostic.",
    key_findings: [
      `Reliability in payload: ${payload.test_results?.reliability ?? "n/a"}`,
      "Consider an in-person exam if symptoms persist."
    ],
    risk_level: "low",
    recommendation: "Share these screening notes with an eye care professional if you have concerns.",
    disclaimer:
      "VisionAssist provides wellness screening only. It does not replace professional eye care."
  };
}

const port = process.env.PORT || 3000;
app.listen(port, () => console.log(`VisionAssist API on http://localhost:${port}`));
