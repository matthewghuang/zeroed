import axios from "axios"

export interface Expense {
  id: number;
  description: string;
  amount: string;
  currency: string;
  baseCurrency: string;
  convertedAmount: string;
  exchangeRate: string;
  rateDate: string;
  date: string;
  paidBy: { id: number; email: string };
  splits: { userId: number; amount: string }[];
}

export default axios.create({
  withCredentials:true,
  xsrfCookieName: "XSRF-TOKEN",
  xsrfHeaderName: "X-XSRF-TOKEN"
});