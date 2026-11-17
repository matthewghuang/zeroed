import { useEffect, useState, } from 'react';
import api from "../api";
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

interface ExpenseListProps {
    groupId: number;
    members: Member[];
    groupName: string;
    onSelectExpense: (expense: Expense) => void;
}


function ExpenseList({ groupId, groupName, members, onSelectExpense  }: ExpenseListProps) {
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

    const getMemberEmail = (userId: number) => {
        const member = members.find(m => m.id === userId);
        return member ? member.email : "Unknown Member";
    }

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
                        <li 
                            key={expense.id} className='list-group-item align-items-center'
                            onClick={() => onSelectExpense(expense)}
                        >
                            <div>
                                <h5 className='mb-1'>{expense.description}</h5>
                                <small>Paid by: {getMemberEmail(expense.paidByUserId)} {expense.date ? `on ${expense.date}` : ''}</small>
                            </div>
                            <div className='text-end'>
                                <span className="fs-5 fw-bold">
                                    {Number(expense.amount).toFixed(2)} {expense.currency}
                                </span>
                            </div>
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
