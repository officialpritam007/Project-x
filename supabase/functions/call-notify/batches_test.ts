import { forEachBounded } from "./batches.ts";

Deno.test("call fan-out visits every device beyond the former 12-device cap", async () => {
  const recipients = Array.from({ length: 29 }, (_, i) => `device-${i}`);
  const seen: string[] = [];
  let active = 0;
  let peak = 0;
  await forEachBounded(recipients, 4, async (device) => {
    active++;
    peak = Math.max(peak, active);
    await Promise.resolve();
    seen.push(device);
    active--;
  });
  if (seen.length !== recipients.length || new Set(seen).size !== recipients.length) {
    throw new Error("Each claimed device must be processed exactly once");
  }
  if (peak > 4 || peak < 2) throw new Error(`Unexpected request concurrency: ${peak}`);
});

Deno.test("empty target set does not send and invalid concurrency is rejected", async () => {
  let sent = 0;
  await forEachBounded<string>([], 12, async () => { sent++; });
  if (sent !== 0) throw new Error("Empty targets must never send");
  let rejected = false;
  try { await forEachBounded([1], 0, async () => {}); }
  catch (e) { rejected = e instanceof RangeError; }
  if (!rejected) throw new Error("Non-positive concurrency must fail fast");
});
