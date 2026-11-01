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

        </div>
    )
}

export default GroupPage