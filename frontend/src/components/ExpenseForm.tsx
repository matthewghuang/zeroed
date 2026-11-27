import { useEffect, useRef, useState, type FormEvent } from "react";
import axios from "axios";
import api, { type Expense } from "../api";
import currencies from "../currencies";
import LoginNotif from "./ErrorNotif";

interface Member {
  id: number;
  email: string;
}

type Quote = Pick<Expense, "baseCurrency" | "convertedAmount" | "exchangeRate" | "rateDate" | "amount" | "currency">;

function requestError(error: unknown, fallback: string) {
  if (!axios.isAxiosError<{ message?: string; fields?: Record<string, string> }>(error)) return fallback;
  const data = error.response?.data;
  return Object.entries(data?.fields ?? {}).map(([field, message]) => `${field}: ${message}`).join("; ")
    || data?.message || fallback;
}

interface ExpenseFormProps {
  groupId: number;
  baseCurrency: string;
  members: Member[];
  expense: Expense | null;
  onExpenseCreated: (expense?: Expense) => void;
}

function ExpenseForm({ groupId, baseCurrency, members, expense, onExpenseCreated }: ExpenseFormProps) {
  const [description, setDescription] = useState(expense?.description ?? "");
  const [amount, setAmount] = useState(expense?.amount ?? "");
  const [currency, setCurrency] = useState(expense?.currency ?? baseCurrency);
  const [date, setDate] = useState(expense?.date ?? "");
  const [paidByUserId, setPaidByUserId] = useState<number | "">(expense?.paidBy.id ?? "");
  const [participantIds, setParticipantIds] = useState<number[]>(expense?.splits.map((split) => split.userId) ?? members.map((member) => member.id));
  const [quote, setQuote] = useState<Quote | null>(expense);
  const [quoteLoading, setQuoteLoading] = useState(false);
  const [quoteError, setQuoteError] = useState("");
  const [quoteAttempt, setQuoteAttempt] = useState(0);
  const [errorMessage, setErrorMessage] = useState("");
  const quoteSequence = useRef(0);

  useEffect(() => {
    const sequence = ++quoteSequence.current;
    setQuoteError("");
    if (expense && amount === expense.amount && currency === expense.currency) {
      setQuote(expense);
      setQuoteLoading(false);
      return;
    }
    setQuote(null);
    if (!amount.trim() || !currency) {
      setQuoteLoading(false);
      return;
    }
    setQuoteLoading(true);
    const timer = window.setTimeout(async () => {
      try {
        const path = expense ? `/api/expenses/${expense.id}/quote` : `/api/groups/${groupId}/expenses/quote`;
        await api.get("/api/auth/csrf");
        const response = await api.post<Quote>(path, { amount, currency });
        if (quoteSequence.current === sequence) setQuote({ ...response.data, amount, currency });
      } catch (error: unknown) {
        if (quoteSequence.current === sequence) setQuoteError(requestError(error, "Unable to get an exchange-rate quote. Please retry."));
      } finally {
        if (quoteSequence.current === sequence) setQuoteLoading(false);
      }
    }, 250);
    return () => {
      window.clearTimeout(timer);
      if (quoteSequence.current === sequence) quoteSequence.current += 1;
    };
  }, [amount, currency, groupId, expense?.id, quoteAttempt]);

  const handleForm = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!quote || quote.amount !== amount || quote.currency !== currency || quoteLoading || quoteError) return;
    try {
      setErrorMessage("");
      await api.get("/api/auth/csrf");
      const payload = {
        description,
        ...(!expense || amount !== expense.amount ? { amount } : {}),
        ...(!expense || currency !== expense.currency ? { currency } : {}),
        ...(date && date !== expense?.date ? { date } : {}),
        ...(paidByUserId !== "" && paidByUserId !== expense?.paidBy.id ? { paidByUserId } : {}),
        ...(!expense || participantIds.length !== expense.splits.length
          || !participantIds.every((id) => expense.splits.some((split) => split.userId === id))
          ? { participantIds } : {})
      };
      if (expense) {
        const response = await api.patch<Expense>(`/api/expenses/${expense.id}`, payload);
        onExpenseCreated(response.data);
      } else {
        await api.post(`/api/groups/${groupId}/expenses`, payload);
        onExpenseCreated();
      }
    } catch (error: unknown) {
      setErrorMessage(requestError(error, "Expense could not be saved. Your entries are still here."));
    }
  };

  return (
    <div className="group-container card border border-secondary text-white mb-3 shadow-lg p-4 overflow-auto" style={{ backgroundColor: "#342835", maxHeight: "100%" }}>
      <form onSubmit={handleForm}>
        <h2>{expense ? "Edit Expense" : "Create Expense"}</h2>
        <div className="form-group">
          <label htmlFor="description" className="form-label">Description:</label>
          <input id="description" className="form-control" value={description} onChange={(e) => setDescription(e.target.value)} />
          <label htmlFor="amount" className="form-label mt-2">Amount ({currency}):</label>
          <input type="number" min="0" step="any" id="amount" className="form-control" value={amount} placeholder="0.00" onChange={(e) => setAmount(e.target.value)} required />
          <label htmlFor="currency" className="form-label mt-2">Currency:</label>
          <select id="currency" className="form-select" value={currency} onChange={(e) => setCurrency(e.target.value)}>
            {currencies.map((code) => <option key={code} value={code}>{code}</option>)}
            {!currencies.includes(currency) && <option value={currency}>{currency}</option>}
          </select>
          {quoteLoading && <p className="mt-2" role="status">Getting exchange rate…</p>}
          {quoteError && (
            <div className="mt-2" role="alert">
              <LoginNotif message={quoteError} />
              <button type="button" className="btn btn-outline-light btn-sm mt-2" onClick={() => setQuoteAttempt((attempt) => attempt + 1)}>Retry quote</button>
            </div>
          )}
          {quote && (
            <div className="mt-2" aria-live="polite">
              <p className="mb-1">Converted total: <strong>{quote.convertedAmount} {quote.baseCurrency}</strong></p>
              {currency !== quote.baseCurrency && <small>Rate: {quote.exchangeRate} ({currency} to {quote.baseCurrency}), effective {quote.rateDate}</small>}
            </div>
          )}
          <label htmlFor="date" className="form-label mt-2">Expense date:</label>
          <input type="date" id="date" className="form-control" value={date} onChange={(e) => setDate(e.target.value)} />
          <label htmlFor="paidBy" className="form-label mt-2">Paid by:</label>
          <select id="paidBy" className="form-select" value={paidByUserId} onChange={(e) => setPaidByUserId(e.target.value ? Number(e.target.value) : "")} required>
            <option value="">Select member…</option>
            {members.map((member) => <option key={member.id} value={member.id}>{member.email}</option>)}
            {expense && !members.some((member) => member.id === expense.paidBy.id) && (
              <option value={expense.paidBy.id}>{expense.paidBy.email} (former member)</option>
            )}
          </select>
          <fieldset className="mt-3">
            <legend className="fs-6">Split among members ({baseCurrency})</legend>
            {members.map((member) => (
              <label key={member.id} className="d-block">
                <input type="checkbox" className="form-check-input me-2" checked={participantIds.includes(member.id)}
                  onChange={(event) => setParticipantIds((current) => event.target.checked ? [...current, member.id] : current.filter((id) => id !== member.id))} />
                {member.email}
              </label>
            ))}
            {expense?.splits.filter((split) => !members.some((member) => member.id === split.userId)).map((split) => (
              <label key={split.userId} className="d-block">
                <input type="checkbox" className="form-check-input me-2" checked={participantIds.includes(split.userId)}
                  onChange={(event) => setParticipantIds((current) => event.target.checked ? [...current, split.userId] : current.filter((id) => id !== split.userId))} />
                Former member #{split.userId}
              </label>
            ))}
          </fieldset>
        </div>
        <div className="text-center mt-3 mb-3">
          <button type="submit" className="btn btn-secondary" disabled={!quote || quote.amount !== amount || quote.currency !== currency || quoteLoading || Boolean(quoteError)}>
            {expense ? "Save Changes" : "Create Expense"}
          </button>
        </div>
        {errorMessage && <LoginNotif message={errorMessage} />}
      </form>
    </div>
  );
}

export default ExpenseForm;
