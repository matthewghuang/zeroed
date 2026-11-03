import { useState, useEffect } from 'react';
import api from "../api";
import ErrorNotif from './ErrorNotif';

interface Member {
    id: number;
    email: string;
}

interface GroupViewProps {
    onSwitchToView: () => void;
    selectedGroup: { id: number; name: string } | null;
}


function GroupView({ selectedGroup }: GroupViewProps) {
    const [members, setMembers] = useState<Member[]>([]);
    const [email, setEmail] = useState('');
    const [errorMessage, setErrorMessage] = useState('');
    const [successMessage, setSuccessMessage] = useState('');
    const [membersError, setMembersError] = useState('');
    
    const fetchMembers = async () => {
        if (!selectedGroup) return;

        try {
            setMembersError('');
            const response = await api.get(`/api/groups/${selectedGroup.id}/members`);
            setMembers(response.data);
        } catch (error) {
            setMembers([]);
            setMembersError('Unable to load group members. Please try again.');
            console.error("Failed to fetch group members", error);
        }
    };

    useEffect(() => {
        setMembers([]);
        fetchMembers();
    }, [selectedGroup]);



    const handleForm = async (e: any) => {
        e.preventDefault();
        if (!selectedGroup) return;

        try {
            setErrorMessage('');
            await api.get('/api/auth/csrf');

            const response = await api.post(`/api/groups/${selectedGroup.id}/members`, { email: email });
            console.log('Group Member Added successfully', response.data);
            setSuccessMessage("Group Member Added Successfully!");
            fetchMembers();
        } catch (error: any) {
            console.error('Group Member Addition failed', error);
            const msg = "Email not authorized as a member."
            setErrorMessage(msg);
        }
    };

    return (
        <div className="container-fluid card border border-secondary text-white mb-3 shadow-lg p-4 h-100" style={{ backgroundColor: '#342835' }}>
            <h1 className="text-center mb-4">{selectedGroup?.name}</h1>
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
            {membersError && <ErrorNotif message={membersError} />}
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

export default GroupView
