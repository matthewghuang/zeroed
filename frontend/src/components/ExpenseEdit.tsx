import { useState, } from 'react';
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

interface ExpenseEditProps {
    selectedGroup: { id: number; name: string } | null;
    selectedExpense: Expense | null;
    members: Member[];
    onBackToList: () => void;
    onExpenseUpdated: () => void;
}


function ExpenseEdit({ selectedGroup, selectedExpense, members, onExpenseUpdated }: ExpenseEditProps) {


    const [errorMessage, setErrorMessage] = useState('');
    const [successMessage, setSuccessMessage] = useState('');

    const getMemberEmail = (userId: number) => {
        const member = members.find(m => m.id === userId);
        return member ? member.email : "Unknown Member";
    }

    return (
        <div></div>
    )
}

export default ExpenseEdit
