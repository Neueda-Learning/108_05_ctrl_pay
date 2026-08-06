import { processQuery, getSuggestions } from './copilotEngine';

describe('Copilot Engine Unit Tests', () => {
  test('getSuggestions returns list of prompt suggestions', () => {
    const suggestions = getSuggestions();
    expect(Array.isArray(suggestions)).toBe(true);
    expect(suggestions.length).toBeGreaterThan(0);
  });

  test('processQuery correctly classifies DASHBOARD_INSIGHTS intent', async () => {
    const response = await processQuery('Show me overall dashboard insights');
    expect(response).toBeDefined();
    expect(response.intent).toBe('DASHBOARD_INSIGHTS');
    expect(response.message).toBeDefined();
  });

  test('processQuery handles GENERAL_HELP fallback', async () => {
    const response = await processQuery('Hello what can you do?');
    expect(response.intent).toBe('GENERAL_HELP');
  });
});
