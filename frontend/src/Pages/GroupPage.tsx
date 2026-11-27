import axios from 'axios';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useState, useEffect } from 'react';
import GroupForm from "../components/GroupForm";
import ExpenseForm from "../components/ExpenseForm";
import ExpenseList from '../components/ExpenseList';
import ExpenseView from '../components/ExpenseView';
import MainHeader from '../components/MainHeader';
import bgImage from '../assets/zeroedBG.png';
import GroupView from '../components/GroupView';
import ErrorNotif from '../components/ErrorNotif';
import api, { type Expense } from "../api";

// properties for each group in group list view
export interface Group {
    id: number;
    name: string;
    baseCurrency: string;
}

interface Member {
    id: number;
    email: string;
}

interface GroupPageProps {
    groups: Group[] | null;
    onGroupsLoaded: (groups: Group[]) => void;
}

function GroupPage({ groups, onGroupsLoaded }: GroupPageProps) {
    const { groupId } = useParams();
    const [currentView, setCurrentView] = useState<'groupView' | 'createGroup' | 'createExpense' | 'expenseList' | 'expenseView'>('groupView');
    const [selectedExpense, setSelectedExpense] = useState<Expense | null>(null);
    const [members, setMembers] = useState<Member[]>([]);
    const [loading, setLoading] = useState(groupId !== undefined || groups === null);
    const [errorMessage, setErrorMessage] = useState('');
    const selectedGroup = groups?.find((group) => String(group.id) === groupId) ?? null;

    const navigate = useNavigate();

    const fetchGroups = async (signal?: AbortSignal) => {
        setLoading(groupId !== undefined || groups === null);
        setErrorMessage('');
        try {
            const response = await api.get<Group[]>('/api/groups', { signal });
            onGroupsLoaded(response.data);

            if (groupId !== undefined) {
                const group = response.data.find((group) => String(group.id) === groupId);
                if (!group) {
                    setErrorMessage('Group not found or you do not have access to it.');
                } else {
                    const response = await api.get<Member[]>(`/api/groups/${group.id}/members`, { signal });
                    setMembers(response.data);
                }
            }
        } catch (error) {
            if (axios.isCancel(error)) return;
            if (axios.isAxiosError(error) && error.response?.status === 401) {
                navigate('/login', {
                    replace: true,
                    state: { from: groupId === undefined ? '/groups' : `/groups/${encodeURIComponent(groupId)}` },
                });
            } else if (axios.isAxiosError(error) && [403, 404].includes(error.response?.status ?? 0)) {
                setErrorMessage('Group not found or you do not have access to it.');
            } else {
                setErrorMessage('Unable to load groups. Please try again later.');
            }
        } finally {
            if (!signal?.aborted) setLoading(false);
        }
    };

    useEffect(() => {
        const controller = new AbortController();
        void fetchGroups(controller.signal);
        return () => controller.abort();
    }, [groupId, navigate]);

    const fetchMembers = async (id: number) => {
        try {
            const response = await api.get<Member[]>(`/api/groups/${id}/members`);
            setMembers(response.data);
        } catch {
            setMembers([]);
            setErrorMessage('Unable to load group members. Return to Groups and try again.');
        }
    };

    const signOut = async () => {
        try {
            await api.get('/api/auth/csrf');
            await api.post('/api/auth/logout');
            navigate("/login")
        } catch (error: any) {
            console.error('Failed to logout', error);
        }
    }

    return (
        <div className="d-flex flex-column vh-100 overflow-hidden" style={{
                backgroundImage: `url(${bgImage})`,
                backgroundSize: 'cover',
                backgroundPosition: 'center',
                backgroundRepeat: 'no-repeat'
            }}>
            <MainHeader />

            {/* Sidebar */}
            <div className='d-flex flex-grow-1 overflow-hidden'>
                <div className="d-flex flex-column align-items-stretch flex-shrink-0 border-end border-black h-100" style={{ width: "200px", backgroundColor: '#211824' }}>
                    {/* Header */}
                    <Link to="/groups" onClick={() => setCurrentView('groupView')} className="d-flex align-items-center flex-shrink-0 p-3 link-body-emphasis text-decoration-none border-bottom border-black" style={{ backgroundColor: '#342835' }}>
                        <span className="fs-5 fw-semibold text-white">Groups</span>
                    </Link>
                    {/* Scrollable Group List */}
                    <div className="list-group list-group-flush border-bottom border-black scrollarea flex-grow-1" style={{ overflowY: 'auto' }}>
                        {/* Items */}
                        <div className="text-center p-4 text-muted" style={{ fontSize: '0.85rem' }}>
                            {groups?.length === 0 ? (
                                <div>No groups yet</div>
                            ) : (
                                groups?.map((group) => (
                                    <Link
                                        key={group.id}
                                        to={`/groups/${group.id}`}
                                        onClick={() => {
                                            setCurrentView('groupView');
                                            setSelectedExpense(null);
                                        }}
                                        aria-current={selectedGroup?.id === group.id ? 'page' : undefined}
                                        className={`list-group-item card border border-secondary text-white mb-3 shadow-lg p-4 ${selectedGroup?.id === group.id ? 'active' : ''}`} style={{ backgroundColor: selectedGroup?.id === group.id ? '#4a3a4d' : '#342835', width: "100%"}}>
                                        {group.name}
                                    </Link>
                                ))
                            )}
                        </div>
                        {/* Create Button (dissappears when making group) */}
                        {currentView === 'groupView' && (
                            <div className="text-center">
                                <button type="button" onClick={() => setCurrentView('createGroup')} className="btn-link">
                                    Create Group
                                </button>
                            </div>
                        )}
                    </div>
                    {/* Bottom Bumper */}
                    <div className="p-3 border-top border-black" style={{ backgroundColor: '#342835', height: '50px' }}>
                        <button type="button" onClick={signOut} className="btn-link">
                            Logout
                        </button>
                    </div>
                </div>
                {/* Main Content Switch*/}
                <main className="d-flex flex-grow-1 justify-content-center align-items-center p-4">
                    {loading ? (
                        <p className="text-white" role="status">Loading groups…</p>
                    ) : errorMessage ? (
                        <ErrorNotif message={errorMessage} />
                    ) : (
                        <>
                    {currentView === 'groupView' && groupId === undefined && (
                        <section className="card border border-secondary text-white shadow-lg p-4" style={{ backgroundColor: '#342835' }}>
                            <h1>Groups</h1>
                            {groups?.length === 0 ? (
                                <p>No groups yet. Create a group to get started.</p>
                            ) : (
                                <ul>
                                    {groups?.map((group) => (
                                        <li key={group.id}>
                                            <Link className="text-white" to={`/groups/${group.id}`}>{group.name}</Link>
                                        </li>
                                    ))}
                                </ul>
                            )}
                        </section>
                    )}
                    {currentView === 'groupView' && selectedGroup && (
                        <GroupView
                            selectedGroup={selectedGroup}
                            members={members}
                            onOpenExpenseForm={() => {
                                setSelectedExpense(null);
                                setCurrentView('createExpense');
                            }}
                            onOpenExpenseList={() => setCurrentView('expenseList')}
                            onMemberAdded={() => fetchMembers(selectedGroup.id)}
                        />
                    )}
                    {currentView === 'createGroup' && (
                        <GroupForm
                            onSwitchToView={() => setCurrentView('groupView')}
                            onGroupCreated={() => {
                                setCurrentView('groupView');
                                void fetchGroups();
                            }}
                        />
                    )}
                    {currentView === 'createExpense' && selectedGroup && (
                        <ExpenseForm
                            groupId={selectedGroup.id}
                            baseCurrency={selectedGroup.baseCurrency}
                            members={members}
                            expense={selectedExpense}
                            onExpenseCreated={(updatedExpense) => {
                                if (updatedExpense) setSelectedExpense(updatedExpense);
                                setCurrentView(selectedExpense ? 'expenseView' : 'groupView');
                            }}
                        />
                    )}
                    {currentView === 'expenseList' && selectedGroup && (
                        <ExpenseList
                            groupId={selectedGroup.id}
                            groupName={selectedGroup.name}
                            onSelectExpense={(expense) => {
                                setSelectedExpense(expense);
                                setCurrentView('expenseView');
                            }}
                        />
                    )}
                    {currentView === 'expenseView' && selectedGroup && (
                        <ExpenseView
                            selectedGroup={selectedGroup}
                            selectedExpense={selectedExpense}
                            members={members}
                            onOpenExpenseEdit={(expense) => {
                                setSelectedExpense(expense);
                                setCurrentView('createExpense');
                            }}
                            onBackToList={() => setCurrentView('expenseList')}
                        />
                    )}
                        </>
                    )}
                </main>
            </div>
        </div>
    )
}

export default GroupPage