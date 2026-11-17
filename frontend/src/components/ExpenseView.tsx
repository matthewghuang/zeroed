import { useState, } from 'react';
import api from "../api";
import ErrorNotif from './ErrorNotif';

interface Member {
    id: number;
    email: string;
}

interface ExpenseViewProps {
    selectedGroup: { id: number; name: string } | null;
    members: Member[]
    onOpenExpenseForm: () => void
    onMemberAdded: () => void; // to fetch members again
}


function ExpenseView({ selectedGroup, members, onOpenExpenseForm }: ExpenseViewProps) {

    return (
        <div className="container-fluid card border border-secondary text-white mb-3 shadow-lg p-4 h-100" style={{ backgroundColor: '#342835' }}>
            <h1 className="text-center mb-4">{selectedGroup?.name}</h1>
            <button
                className="btn btn-outline-light"
                onClick={onOpenExpenseForm}
            >
                Edit Expense
            </button>
            {/* Add members */}
            <form onSubmit={handleForm} className="mb-4">
                <div className="input-group">
                    <input 
                        type="email" 
                        className="form-control"
                        placeholder="email"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                    />
                    <button type="submit" className="btn btn-outline-light">Add Member</button>
                </div>
                {successMessage && <ErrorNotif message={successMessage} type="success" />}
                {errorMessage && <ErrorNotif message={errorMessage} />}
            </form>

            {/* Member List */}
            <h4 className="border-bottom pb-2">Members</h4>
            <ul className="list-group list-group-flush bg-transparent">
                {members.map((member) => (
                    <li key={member.id} className="list-group-item bg-transparent text-white border-secondary">
                        {member.email}
                    </li>
                ))}
            </ul>
        </div>
    )
}

export default ExpenseView
