import { useEffect, useState, } from 'react';
import api, { type Expense } from "../api";
import ErrorNotif from './ErrorNotif';

interface ExpenseListProps {
    groupId: number;
    groupName: string;
    onSelectExpense: (expense: Expense) => void;
}


function ExpenseList({ groupId, groupName, onSelectExpense }: ExpenseListProps) {
    const [expenses, setExpenses] = useState<Expense[]>([]);
    const [errorMessage, setErrorMessage] = useState("");

    useEffect(() => {
        const fetchExpenses = async () => {
            if (!groupId) {
                setExpenses([])
                return;
            }
            try {
                setErrorMessage("");
                const response = await api.get(`/api/groups/${groupId}/expenses`);
                setExpenses(response.data);
            } catch (error) {
                console.error("Failed to load expenses.");
                setErrorMessage("Failed to load expenses.")
                setExpenses([]);
            }
        };

        fetchExpenses();
    }, [groupId])

    return (
        <div className="container-fluid card border border-secondary text-white mb-3 shadow-lg p-4 h-100" style={{ backgroundColor: '#342835' }}>
            <h1 className="text-center mb-4">{groupName}</h1>
            <h4 className="border-bottom pb-2 mb-3">Expenses</h4>
            {errorMessage && <ErrorNotif message={errorMessage}/>}

            {/* Expense List of the group */}
            {expenses.length === 0 ? (
                <div className='text-center p-4'>No expenses recorded yet</div>
            ) : (
                <ul className='list-group list-group-flush bg-transparent'>
                    {expenses.map((expense) => (
                        <li key={expense.id} className='list-group-item align-items-center'>
                            <button type="button" className="btn btn-link text-start text-decoration-none text-reset w-100 p-0" onClick={() => onSelectExpense(expense)}>
                                <div>
                                    <h5 className='mb-1'>{expense.description}</h5>
                                    <small>Paid by: {expense.paidBy.email} {expense.date ? `on ${expense.date}` : ''}</small>
                                </div>
                                <div className='text-end'>
                                    <span className="fs-5 fw-bold">{expense.amount} {expense.currency}</span>
                                    <div>{expense.convertedAmount} {expense.baseCurrency}</div>
                                    {expense.currency !== expense.baseCurrency && <small>Rate {expense.exchangeRate}, effective {expense.rateDate}</small>}
                                </div>
                            </button>
                        </li>
                    )
                )}
                </ul>
            )
        }
        </div>
    )
}

export default ExpenseList
