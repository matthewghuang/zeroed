import { useState } from 'react';
import api from "../api";
import LoginNotif from './ErrorNotif';

interface GroupFormProps {
    onSwitchToView: () => void;
}

function GroupForm({ onSwitchToView }: GroupFormProps) {
    const [groupName, setGroupName] = useState('')

    const [errorMessage, setErrorMessage] = useState('');
    const [successMessage, setSuccessMessage] = useState('');

    const handleForm = async (e: any) => {
        e.preventDefault();

        try {
            setErrorMessage('');
            await api.get('/api/auth/csrf');

            const response = await api.post('/api/groups', { name: groupName });
            console.log('Group creation successful', response.data);
            setSuccessMessage("Group Creation Successful!");
            await new Promise(resolve => setTimeout(resolve, 2000));
            onSwitchToView()
        } catch (error: any) {
            console.error('Group creation failed', error);
            const msg = "Group creation failed. Empty box."
            setErrorMessage(msg);
        }
    };

    return (
        <div className="group-container card border border-secondary text-white mb-3 shadow-lg p-4" style={{ backgroundColor: '#342835' }}>
            <form onSubmit={handleForm}>
                <div className="form-group">
                    <label htmlFor="text" className="form-label">Group Name:</label>
                    <input
                        type="text"
                        id="text"
                        className="form-control"
                        value={groupName}
                        placeholder='text'
                        onChange={(e) => setGroupName(e.target.value)}
                    ></input>
                </div>
                <div className="text-center mt-3 mb-3">
                    <button type="submit" className="btn btn-secondary text-center">
                        Create Group
                    </button>
                </div>
                {successMessage && <LoginNotif message={successMessage} type="success" />}
                {errorMessage && <LoginNotif message={errorMessage} />}
            </form>
        </div>
    )
}

export default GroupForm