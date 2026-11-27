import { useEffect, useState } from 'react';
import api from '../api';

interface MemberProfile {
    id: number;
    email: string;
}

interface MemberBalance {
    userId: number;
    paid: string;
    owed: string;
    net: string;
}

interface Transfer {
    from: MemberProfile;
    to: MemberProfile;
    amount: string;
}

interface CurrencyBalance {
    currency: string;
    members: MemberBalance[];
    transfers: Transfer[];
}

interface GroupBalanceResponse {
    groupId: number;
    members: MemberProfile[];
    currencies: CurrencyBalance[];
    generatedAt: string;
}

interface GroupBalanceProps {
    groupId: number;
    baseCurrency: string;
    refreshKey: number;
}

function GroupBalance({ groupId, baseCurrency, refreshKey }: GroupBalanceProps) {

    const [balance, setBalance] = useState<GroupBalanceResponse | null>(null);
    const [error, setError] = useState(false);

    useEffect(() => {
        let current = true;
        setBalance(null);
        setError(false);

        api.get<GroupBalanceResponse>(`/api/groups/${groupId}/balances`)
            .then((response) => {
                if (current) setBalance(response.data);
            })
            .catch(() => {
                if (current) setError(true);
            });

        return () => {
            current = false;
        };
    }, [groupId, refreshKey]);

    return (
        <section className="border-top border-secondary mt-4 pt-3" aria-labelledby="group-balance-heading">
            <h4 id="group-balance-heading" className="border-bottom border-secondary pb-2 mb-3">Balances ({baseCurrency})</h4>
            {error ? (
                <p className="alert alert-danger">Failed to load group balances.</p>
            ) : balance === null ? (
                <p className="text-white-50" role="status">Loading balances…</p>
            ) : balance.currencies.length === 0 ? (
                <p className="text-white-50">No expenses recorded yet.</p>
            ) : (
                <div className="d-flex flex-column gap-3">
                    {balance.currencies.filter((section) => section.currency === baseCurrency).map((currency) => (
                        <section key={currency.currency} className="border border-secondary rounded p-3">
                            <div className="table-responsive">
                                <table className="table table-dark table-sm align-middle mb-3">
                                    <thead>
                                        <tr>
                                            <th scope="col">Member</th>
                                            <th scope="col" className="text-end">Paid</th>
                                            <th scope="col" className="text-end">Owed</th>
                                            <th scope="col" className="text-end">Net</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {currency.members.map((member) => {
                                            const profile = balance.members.find((item) => item.id === member.userId);
                                            const netClass = member.net.startsWith('-')
                                                ? 'text-danger'
                                                : member.net === '0.00' || /^0(?:\.0+)?$/.test(member.net)
                                                    ? 'text-white-50'
                                                    : 'text-success';
                                            return (
                                                <tr key={member.userId}>
                                                    <th scope="row" className="fw-normal">{profile?.email ?? `Member ${member.userId}`}</th>
                                                    <td className="text-end">{member.paid}</td>
                                                    <td className="text-end">{member.owed}</td>
                                                    <td className={`text-end fw-semibold ${netClass}`}>{member.net}</td>
                                                </tr>
                                            );
                                        })}
                                    </tbody>
                                </table>
                            </div>
                            <h6>Suggested transfers</h6>
                            {currency.transfers.length === 0 ? (
                                <p className="text-white-50 mb-0">Everyone is settled.</p>
                            ) : (
                                <ul className="list-group list-group-flush">
                                    {currency.transfers.map((transfer, index) => (
                                        <li key={`${transfer.from.id}-${transfer.to.id}-${index}`}
                                            className="list-group-item bg-transparent text-white border-secondary px-0">
                                            <span>{transfer.from.email} pays {transfer.to.email}</span>
                                            <strong className="float-end">{transfer.amount}</strong>
                                        </li>
                                    ))}
                                </ul>
                            )}
                        </section>
                    ))}
                </div>
            )}
        </section>
    );
}

export default GroupBalance;
