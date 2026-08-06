import React from 'react';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import CreatePayment from './CreatePayment';
import { CustomerContext } from '../context/CustomerContext';

const mockCustomerContext = {
  customer: { id: 1, name: 'John Doe' },
  accounts: [{ accountNumber: '123456789012', accountName: 'John Doe-Savings', currency: 'USD', accountBalance: 1000 }],
  activeAccount: { accountNumber: '123456789012', accountName: 'John Doe-Savings', currency: 'USD', accountBalance: 1000 },
  setActiveAccount: jest.fn(),
  refreshCustomer: jest.fn(),
};

describe('CreatePayment Page Component Tests', () => {
  test('renders Create Payment stepper and steps', () => {
    render(
      <MemoryRouter>
        <CustomerContext.Provider value={mockCustomerContext}>
          <CreatePayment />
        </CustomerContext.Provider>
      </MemoryRouter>
    );

    expect(screen.getByText(/Create Payment/i)).toBeInTheDocument();
    expect(screen.getByText(/Verify Destination Account/i)).toBeInTheDocument();
    expect(screen.getByText(/Currency & Amount/i)).toBeInTheDocument();
    expect(screen.getByText(/Review & Authenticate/i)).toBeInTheDocument();
  });
});
