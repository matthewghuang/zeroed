import { useState } from 'react';
import { BrowserRouter, Routes, Route, Navigate, useParams } from 'react-router-dom';
import LoginPage from './Pages/LoginPage';
import GroupPage, { type Group } from './Pages/GroupPage';

function GroupRoute() {
    const { groupId } = useParams();
    const [groups, setGroups] = useState<Group[] | null>(null);
    return (
        <GroupPage
            key={groupId === undefined ? 'overview' : `group:${groupId}`}
            groups={groups}
            onGroupsLoaded={setGroups}
        />
    );
}

function App() {

  return (
    <BrowserRouter>
        <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/groups/:groupId/*" element={<GroupRoute />} />
            <Route path="/groups" element={<GroupRoute />} />
            <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
    </BrowserRouter>
    )
}

export default App
