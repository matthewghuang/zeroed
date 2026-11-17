import { useState } from "react";
import api from "../api";
import LoginNotif from "./ErrorNotif";

interface Member {
    id: number;
    email: string;
}

interface ExpenseFormProps {
  groupId: number;
  members: Member[]; // from GroupView
  onSwitchToView: () => void;
  onExpenseCreated?: () => void;
}

function ExpenseForm({ groupId, members, onSwitchToView, onExpenseCreated }: ExpenseFormProps) {
  const [description, setDescription] = useState("");
  const [amount, setAmount] = useState("");
  const [currency, setCurrency] = useState("CAD");
  const [date, setDate] = useState("");
  const [paidByUserId, setPaidByUserId] = useState<number | "">("");
  
  const participantIds = members.map(m => m.id)


  const [errorMessage, setErrorMessage] = useState("");
  const [successMessage, setSuccessMessage] = useState("");

  const handleForm = async (e: any) => {
    e.preventDefault();

    try {
      setErrorMessage("");
      await api.get("/api/auth/csrf");
      const response = await api.post(`/api/groups/${groupId}/expenses`, { 
        description,
        amount: amount ? Number(amount).toFixed(2) : "", // 2 decimals
        currency,
        ...(date && {date}),
        ...(paidByUserId !== "" && {paidByUserId}),
        participantIds
      });

      if (onExpenseCreated) {
        onExpenseCreated();
      }

      console.log("Expense added successfully", response.data);
      setSuccessMessage("Expense added successfully");
      await new Promise((resolve) => setTimeout(resolve, 2000));
      onSwitchToView();
    } catch (error: any) {
      console.error("Expense creation failed", error);
      const msg = "Expense creation failed.";
      setErrorMessage(msg);
    }
  };

  return (
    <div
      className="group-container card border border-secondary text-white mb-3 shadow-lg p-4"
      style={{ backgroundColor: "#342835" }}
    >
      <form onSubmit={handleForm}>
        <div className="form-group">
          <label htmlFor="description" className="form-label">
            Description:
          </label>
          <input
            type="text"
            id="description"
            className="form-control"
            value={description}
            placeholder="description..."
            onChange={(e) => setDescription(e.target.value)}
          ></input>
          <label htmlFor="amount" className="form-label">
            amount:
          </label>
          <input
            type="number"
            step="0.01"
            id="amount"
            className="form-control"
            value={amount}
            placeholder="0.00"
            onChange={(e) => setAmount(e.target.value)}
          ></input>
          <label htmlFor="currency" className="form-label">
            currency:
          </label>
          <select 
            id="currency"
            className="form-select"
            value={currency}
            onChange={(e) => setCurrency(e.target.value)}
          >
            <option value="CAD">CAD</option>
            <option value="USD">USD</option>
            <option value="EUR">EUR</option>
          </select>
          <label htmlFor="date" className="form-label">
            date:
          </label>
          <input 
            type="date"
            id="date"
            className="form-control"
            value={date}
            placeholder="description..."
            onChange={(e) => setDate(e.target.value)}
          />
          <label htmlFor="paidBy" className="form-label">
            Paid By:
          </label>
          <select
            id="paidBy"
            className="form-select"
            value={paidByUserId}
            onChange={(e) => setPaidByUserId(e.target.value ? Number(e.target.value) : "")}
            required
          >
            <option value="">Select member...</option>
            {members.map((member) => (
              <option key={member.id} value={member.id}>
                {member.email}
              </option>
            )
          )}
          </select>
        </div>
        <div className="text-center mt-3 mb-3">
          <button type="submit" className="btn btn-secondary text-center">
            Create Expense
          </button>
        </div>
        {successMessage && (
          <LoginNotif message={successMessage} type="success" />
        )}
        {errorMessage && <LoginNotif message={errorMessage} />}
      </form>
    </div>
  );
}

export default ExpenseForm;
