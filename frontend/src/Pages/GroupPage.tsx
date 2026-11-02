import MainHeader from '../components/MainHeader';
import bgImage from '../assets/zeroedBG.png';

function GroupPage() {
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
                    <a href="/" className="d-flex align-items-center flex-shrink-0 p-3 link-body-emphasis text-decoration-none border-bottom border-black" style={{ backgroundColor: '#342835' }}>
                        <span className="fs-5 fw-semibold text-white">Groups</span>
                    </a>
                    {/* Scrollable List Container */}
                    <div className="list-group list-group-flush border-bottom border-black scrollarea flex-grow-1" style={{ overflowY: 'auto' }}>
                        {/* Items */}
                        <div className="text-center p-4 text-muted" style={{ fontSize: '0.85rem' }}>
                            <button className='list-group-item list-group-item-action active py-3 lh-sm'>
                                group 1
                            </button>
                            <button className='list-group-item list-group-item-action active py-3 lh-sm'>
                                group 2
                            </button>
                        </div>
                        {/* Create Button */}
                        <div className="text-center">
                            <button type="submit" className="btn btn-secondary text-center">
                                Create Group
                            </button>
                        </div>
                    </div>
                    {/* Bottom Bumper */}
                    <div className="p-3 border-top border-black" style={{ backgroundColor: '#342835', height: '50px' }}></div>
                </div>
            </div>

            {/* Main Content */}
            <div>
                
            </div>
        </div>
    )
}

export default GroupPage