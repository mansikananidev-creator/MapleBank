import { NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '@/context/AuthContext'
import { Button } from '@/components/ui/button'
import {ThemeToggle} from "@/components/ThemeToggle.tsx";

export default function NavBar() {
    const { user, logout } = useAuth()
    const navigate = useNavigate()

    function handleLogout() {
        logout()
        navigate('/login')
    }

    const linkClass = ({ isActive }: { isActive: boolean }) =>
        `text-sm transition-colors hover:text-foreground ${
            isActive ? 'text-foreground font-medium' : 'text-muted-foreground'
        }`

    return (
        <nav className="border-b border-border bg-background">
            <div className="max-w-5xl mx-auto px-6 h-14 flex items-center justify-between">
                <div className="flex items-center gap-6">
                    <span className="font-bold">Maple Bank</span>
                    <NavLink to="/dashboard" className={linkClass}>Dashboard</NavLink>
                    <NavLink to="/accounts" className={linkClass}>Accounts</NavLink>
                    <NavLink to="/transactions" className={linkClass}>Transactions</NavLink>
                    <NavLink to="/loans" className={linkClass}>Loans</NavLink>
                    {user?.role === 'ADMIN' && (
                        <NavLink to="/admin/loans" className={linkClass}>Admin</NavLink>
                    )}
                </div>

                <ThemeToggle />

                <div className="flex items-center gap-4">
                    <span className="text-sm text-muted-foreground">{user?.email}</span>
                    <Button variant="outline" size="sm" onClick={handleLogout}>Log out</Button>
                </div>
            </div>
        </nav>
    )
}