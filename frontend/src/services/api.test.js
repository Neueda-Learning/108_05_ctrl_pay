import apiClient, { paymentAPI, rulesAPI, customerAPI, accountAPI, currencyAPI } from './api';

jest.mock('axios', () => {
  const mAxios = {
    create: jest.fn(() => mAxios),
    get: jest.fn(),
    post: jest.fn(),
    put: jest.fn(),
    patch: jest.fn(),
    delete: jest.fn(),
  };
  return mAxios;
});

describe('API Services Unit Tests', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  test('paymentAPI.createPayment calls POST /payments', async () => {
    const payload = { amount: 100, currency: 'USD' };
    apiClient.post.mockResolvedValue({ data: { paymentId: 1 } });

    const result = await paymentAPI.createPayment(payload);

    expect(apiClient.post).toHaveBeenCalledWith('/payments', payload);
    expect(result.data).toEqual({ paymentId: 1 });
  });

  test('rulesAPI.listRules calls GET /admin/validation-rules', async () => {
    apiClient.get.mockResolvedValue({ data: [] });

    const result = await rulesAPI.listRules();

    expect(apiClient.get).toHaveBeenCalledWith('/admin/validation-rules');
    expect(result.data).toEqual([]);
  });

  test('customerAPI.createCustomer calls POST /customers', async () => {
    const payload = { name: 'Alice' };
    apiClient.post.mockResolvedValue({ data: { id: 1 } });

    const result = await customerAPI.createCustomer(payload);

    expect(apiClient.post).toHaveBeenCalledWith('/customers', payload);
    expect(result.data).toEqual({ id: 1 });
  });

  test('accountAPI.createAccount calls POST /customers/1/accounts', async () => {
    const payload = { accountNumber: '123456789012' };
    apiClient.post.mockResolvedValue({ data: { accountId: 10 } });

    const result = await accountAPI.createAccount(1, payload);

    expect(apiClient.post).toHaveBeenCalledWith('/customers/1/accounts', payload);
    expect(result.data).toEqual({ accountId: 10 });
  });

  test('currencyAPI.getCurrencies calls GET /currencies', async () => {
    apiClient.get.mockResolvedValue({ data: [{ code: 'USD', name: 'US Dollar' }] });

    const result = await currencyAPI.getCurrencies();

    expect(apiClient.get).toHaveBeenCalledWith('/currencies');
    expect(result.data).toEqual([{ code: 'USD', name: 'US Dollar' }]);
  });
});
