import { useState, } from 'react';
import ErrorNotif from './ErrorNotif';

interface Member {
    id: number;
    email: string;
}
interface Expense {
    id: number;
    description: string;
    amount: number;
    currency: string;
    date: string;
    paidByUserId: number;
}


interface ExpenseViewProps {
    selectedGroup: { id: number; name: string } | null;
    selectedExpense: Expense | null;
    members: Member[]
    onOpenExpenseEdit: () => void
    onBackToList: () => void;
}


function ExpenseView({ selectedGroup, selectedExpense, members, onOpenExpenseEdit, onBackToList }: ExpenseViewProps) {
    const [errorMessage] = useState('');

    const getMemberEmail = (userId: number) => {
        const member = members.find(m => m.id === userId);
        return member ? member.email : "Unknown Member";
    }

    return (
        <div className="container-fluid card border border-secondary text-white mb-3 shadow-lg p-4 h-100" style={{ backgroundColor: '#342835' }}>
            <h1 className="text-center mb-4">{selectedGroup?.name}</h1>
            <button
                className="btn btn-outline-light"
                onClick={onOpenExpenseEdit}
            >
                Edit Expense
            </button>
            <button
                className="btn btn-outline-light"
                onClick={onBackToList}
            >
                Back
            </button>

            {/* Display Members and expense and their info */}

            {errorMessage && <ErrorNotif message={errorMessage} />}
`           {selectedExpense ? (
                <div className="card border-secondary p-4">
                    <h3 className="mb-2">{selectedExpense.description}</h3>
                    <p className="fs-4 fw-bold text-success mb-3">
                        {Number(selectedExpense.amount).toFixed(2)} {selectedExpense.currency}
                    </p>
                    <p className="text-muted mb-4">
                        Paid by: <span className="text-white">{getMemberEmail(selectedExpense.paidByUserId)}</span> on {selectedExpense.date}
                    </p>

                    <h4 className="border-bottom pb-2 mb-3">Split Breakdown</h4>
                    <p className="text-muted"></p>
                </div>
            ) : (
                <div className="text-center p-4 text-muted">No expense selected</div>
            )}
        </div>
    )
}

export default ExpenseView
