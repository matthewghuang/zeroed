import api from "../api";

interface GroupViewProps {
    onSwitchToView: () => void;
    selectedGroup: { id: number; name: string } | null;
}


function GroupView({ onSwitchToView, selectedGroup }: GroupViewProps) {
    return (
        <div className="container-fluid card border border-secondary text-white mb-3 shadow-lg p-4 h-100" style={{ backgroundColor: '#342835' }}>
            <h1 className="text-center">{selectedGroup?.name}</h1>
        </div>
    )
}

export default GroupView