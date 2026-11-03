import { useNavigate } from "react-router-dom";
import { useState, useEffect } from "react";
import GroupForm from "../components/GroupForm";
import MainHeader from "../components/MainHeader";
import bgImage from "../assets/zeroedBG.png";
import GroupView from "../components/GroupView";
import api from "../api";
import type { CurrentUser } from "../api";

// properties for each group in group list view
interface Group {
  id: number;
  name: string;
}

interface GroupPageProps {
  currentUser: CurrentUser | null;
  onUserLoaded: (user: CurrentUser) => void;
}

function GroupPage({ currentUser, onUserLoaded }: GroupPageProps) {
  const [isViewing, setIsViewing] = useState(true);
  const [groups, setGroups] = useState<Group[]>([]);
  const [selectedGroup, setSelectedGroup] = useState<Group | null>(null);

  const navigate = useNavigate();

  const fetchGroups = async () => {
    try {
      const response = await api.get("/api/groups");
      const userGroups: Group[] = response.data;
      setGroups(userGroups);

      // user is in atleast 1 group
      if (userGroups.length > 0) {
        setSelectedGroup(userGroups[0]); // first group visible
        setIsViewing(true); // make the groups view active
      } else {
        setIsViewing(false); // make the create view active
      }
    } catch (error) {
      console.error("Failed to fetch groups", error);
    }
  };
  useEffect(() => {
    fetchGroups();
  }, []);

  const signOut = async () => {
    try {
      await api.get("/api/auth/csrf");
      await api.post("/api/auth/logout");
      navigate("/login");
    } catch (error: any) {
      console.error("Failed to logout", error);
    }
  };

  return (
    <div
      className="d-flex flex-column vh-100 overflow-hidden"
      style={{
        backgroundImage:
          currentUser?.role === "ADMIN"
            ? "radial-gradient(circle at center, #210b10 0%, #6b1728 50%, #b83742 100%)"
            : `url(${bgImage})`,
        backgroundSize: "cover",
        backgroundPosition: "center",
        backgroundRepeat: "no-repeat",
      }}
    >
      <MainHeader />

      {/* Sidebar */}
      <div className="d-flex flex-grow-1 overflow-hidden">
        <div
          className="d-flex flex-column align-items-stretch flex-shrink-0 border-end border-black h-100"
          style={{ width: "200px", backgroundColor: "#211824" }}
        >
          {/* Header */}
          <a
            className="d-flex align-items-center flex-shrink-0 p-3 link-body-emphasis text-decoration-none border-bottom border-black"
            style={{ backgroundColor: "#342835" }}
          >
            <span className="fs-5 fw-semibold text-white">Groups</span>
          </a>
          {/* Scrollable Group List */}
          <div
            className="list-group list-group-flush border-bottom border-black scrollarea flex-grow-1"
            style={{ overflowY: "auto" }}
          >
            {/* Items */}
            <div
              className="text-center p-4 text-muted"
              style={{ fontSize: "0.85rem" }}
            >
              {groups.length === 0 ? (
                <div>No groups yet</div>
              ) : (
                groups.map((group) => (
                  <button
                    key={group.id}
                    onClick={() => {
                      setSelectedGroup(group);
                      setIsViewing(true);
                    }}
                    className={`list-group-item card border border-secondary text-white mb-3 shadow-lg p-4 ${selectedGroup?.id === group.id ? "active" : ""}`}
                    style={{
                      backgroundColor:
                        selectedGroup?.id === group.id ? "#4a3a4d" : "#342835",
                      width: "100%",
                    }}
                  >
                    {group.name}
                  </button>
                ))
              )}
            </div>
            {/* Create Button (dissappears when making group) */}
            {isViewing ? (
              <div className="text-center">
                <button
                  type="button"
                  onClick={() => setIsViewing(false)}
                  className="btn-link"
                >
                  Create Group
                </button>
              </div>
            ) : null}
          </div>
          {/* Bottom Bumper */}
          <div
            className="p-3 border-top border-black"
            style={{ backgroundColor: "#342835", height: "50px" }}
          >
            <button type="button" onClick={signOut} className="btn-link">
              Logout
            </button>
          </div>
        </div>
        <main className="d-flex flex-grow-1 justify-content-center align-items-center p-4">
          {isViewing ? (
            <GroupView
              onSwitchToView={() => setIsViewing(false)}
              selectedGroup={selectedGroup}
            />
          ) : (
            <GroupForm
              onSwitchToView={() => setIsViewing(true)}
              onGroupCreated={fetchGroups}
            />
          )}
        </main>
      </div>
      {/* Main Content */}
    </div>
  );
}

export default GroupPage;
