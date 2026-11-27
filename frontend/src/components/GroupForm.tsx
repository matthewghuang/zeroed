import { useState } from "react";
import api from "../api";
import currencies from "../currencies";
import LoginNotif from "./ErrorNotif";

interface GroupFormProps {
  onSwitchToView: () => void;
  onGroupCreated?: () => void;
}

function GroupForm({ onSwitchToView, onGroupCreated }: GroupFormProps) {
  const [groupName, setGroupName] = useState("");
  const [baseCurrency, setBaseCurrency] = useState("CAD");

  const [errorMessage, setErrorMessage] = useState("");
  const [successMessage, setSuccessMessage] = useState("");

  const handleForm = async (e: any) => {
    e.preventDefault();

    try {
      setErrorMessage("");
      await api.get("/api/auth/csrf");
      const response = await api.post("/api/groups", { name: groupName, baseCurrency });

      if (onGroupCreated) {
        onGroupCreated();
      }

      console.log("Group creation successful", response.data);
      setSuccessMessage("Group Creation Successful!");
      await new Promise((resolve) => setTimeout(resolve, 2000));
      onSwitchToView();
    } catch (error: any) {
      console.error("Group creation failed", error);
      const msg = "Group creation failed. Empty box.";
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
          <label htmlFor="groupName" className="form-label">
            Group Name:
          </label>
          <input
            type="text"
            id="groupName"
            className="form-control"
            value={groupName}
            placeholder="group name..."
            onChange={(e) => setGroupName(e.target.value)}
          ></input>
        <div className="form-group mt-3">
          <label htmlFor="baseCurrency" className="form-label">Base currency:</label>
          <select
            id="baseCurrency"
            className="form-select"
            value={baseCurrency}
            onChange={(e) => setBaseCurrency(e.target.value)}
            required
          >
            {currencies.map((currency) => <option key={currency} value={currency}>{currency}</option>)}
          </select>
        </div>
        </div>
        <div className="text-center mt-3 mb-3">
          <button type="submit" className="btn btn-secondary text-center">
            Create Group
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

export default GroupForm;
