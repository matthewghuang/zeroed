import api from "../api";

interface GroupFormProps {
    onSwitchToView: () => void;
}


function GroupView({ onSwitchToView }: GroupFormProps) {
    return (
        <div className="container-fluid card border border-secondary text-white mb-3 shadow-lg p-4 h-100" style={{ backgroundColor: '#342835' }}>
            <h1 className="text-center">Group Name</h1>
        </div>
    )
}

export default GroupView