import { defineConfig, devices } from '@playwright/test';

const baseURL = 'http://127.0.0.1:18080';

export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  workers: 1,
  forbidOnly: !!process.env.CI,
  retries: 0,
  reporter: [['list'], ['html', { open: 'never' }]],
  expect: { timeout: 10_000 },
  use: {
    baseURL,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: {
    command: 'docker compose -p zeroed-e2e -f ../compose.e2e.yaml up --build --abort-on-container-exit',
    url: `${baseURL}/actuator/health`,
    reuseExistingServer: false,
    timeout: 300_000,
    gracefulShutdown: { signal: 'SIGTERM', timeout: 15_000 },
  },
});
