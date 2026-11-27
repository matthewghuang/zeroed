import { useEffect, useState } from 'react';
import api, { type Expense } from '../api';
import ErrorNotif from './ErrorNotif';

interface Member {
    id: number;
    email: string;
}

interface ExpenseViewProps {
    selectedGroup: { id: number; name: string } | null;
    selectedExpense: Expense | null;
    members: Member[]
    onOpenExpenseEdit: (expense: Expense) => void;
    onBackToList: () => void;
}


function ExpenseView({ selectedGroup, selectedExpense, members, onOpenExpenseEdit, onBackToList }: ExpenseViewProps) {
    const [expense, setExpense] = useState<Expense | null>(null);
    const [errorMessage, setErrorMessage] = useState('');

    useEffect(() => {
        if (!selectedExpense) {
            setExpense(null);
            return;
        }
        let active = true;
        setExpense(null);
        setErrorMessage('');
        api.get<Expense>(`/api/expenses/${selectedExpense.id}`)
            .then((response) => {
                if (active) setExpense(response.data);
            })
            .catch(() => {
                if (active) setErrorMessage('Unable to load expense details. Please try again.');
            });
        return () => { active = false; };
    }, [selectedExpense?.id]);

    const getMemberEmail = (userId: number) => {
        const member = members.find(m => m.id === userId);
        return member ? member.email : "Unknown Member";
    }

    return (
        <div className="container-fluid card border border-secondary text-white mb-3 shadow-lg p-4 h-100" style={{ backgroundColor: '#342835' }}>
            <h1 className="text-center mb-4">{selectedGroup?.name}</h1>
            <button className="btn btn-outline-light" onClick={() => expense && onOpenExpenseEdit(expense)} disabled={!expense}>Edit Expense</button>
            <button className="btn btn-outline-light" onClick={onBackToList}>Back</button>
            {errorMessage && <ErrorNotif message={errorMessage} />}
            {!expense && selectedExpense && !errorMessage && <p role="status">Loading expense…</p>}
            {expense ? (
                <div className="card border-secondary p-4">
                    <h3 className="mb-2">{expense.description}</h3>
                    <p className="fs-4 fw-bold text-success mb-1">{expense.amount} {expense.currency}</p>
                    <p className="fs-5 fw-semibold mb-2">{expense.convertedAmount} {expense.baseCurrency}</p>
                    {expense.currency !== expense.baseCurrency && (
                        <p className="text-muted mb-3">Exchange rate: {expense.exchangeRate} ({expense.currency} to {expense.baseCurrency}), effective {expense.rateDate}</p>
                    )}
                    <p className="text-muted mb-4">
                        Paid by: <span>{expense.paidBy.email}</span> on {expense.date}
                    </p>
                    <h4 className="border-bottom pb-2 mb-3">Split Breakdown ({expense.baseCurrency})</h4>
                    {expense.splits?.map((split) => (
                        <p key={split.userId} className="mb-1">{getMemberEmail(split.userId)}: {split.amount} {expense.baseCurrency}</p>
                    ))}
                </div>
            ) : !selectedExpense ? (
                <div className="text-center p-4 text-muted">No expense selected</div>
            ) : null}
        </div>
    )
}

export default ExpenseView
