import '@testing-library/jest-dom/vitest';

if (typeof window !== 'undefined') {
  class MemoryStorage implements Storage {
    private store: Record<string, string> = {};
    get length() { return Object.keys(this.store).length; }
    clear() { this.store = {}; }
    getItem(key: string) { return this.store[key] ?? null; }
    key(index: number) { return Object.keys(this.store)[index] ?? null; }
    removeItem(key: string) { delete this.store[key]; }
    setItem(key: string, value: string) { this.store[key] = String(value); }
  }
  const storage = new MemoryStorage();
  Object.defineProperty(globalThis, 'localStorage', {
    value: storage,
    writable: true,
    configurable: true,
  });
}

