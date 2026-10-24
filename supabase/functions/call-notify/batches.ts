/** Process every claimed device without creating an unbounded FCM request burst. */
export async function forEachBounded<T>(
  items: readonly T[],
  concurrency: number,
  process: (item: T) => Promise<void>,
): Promise<void> {
  if (!Number.isSafeInteger(concurrency) || concurrency < 1) {
    throw new RangeError("Concurrency must be a positive integer");
  }
  let next = 0;
  await Promise.all(Array.from({ length: Math.min(concurrency, items.length) }, async () => {
    while (next < items.length) {
      const item = items[next++]!;
      await process(item);
    }
  }));
}
