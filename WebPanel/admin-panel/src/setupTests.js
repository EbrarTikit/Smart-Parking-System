// src/setupTests.js
import '@testing-library/jest-dom';

// Mock window.matchMedia
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: jest.fn().mockImplementation(query => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: jest.fn(),
    removeListener: jest.fn(),
    addEventListener: jest.fn(),
    removeEventListener: jest.fn(),
    dispatchEvent: jest.fn(),
  })),
});

// Mock IntersectionObserver
global.IntersectionObserver = class IntersectionObserver {
  constructor() {}
  observe() { return null; }
  unobserve() { return null; }
  disconnect() { return null; }
};

// Console log'ları test sırasında gösterme
const originalLog = console.log;
console.log = (...args) => {
  // Test sırasında log'ları gösterme
  return;
};

// Console error'ları test sırasında gösterme
const originalError = console.error;
console.error = (...args) => {
  if (args[0]?.includes('API Error')) return;
  originalError.call(console, ...args);
};

// Add this to suppress React Router v7 warnings
const originalWarn = console.warn;
console.warn = (...args) => {
  if (args[0]?.includes('React Router Future Flag Warning')) return;
  originalWarn.call(console, ...args);
};