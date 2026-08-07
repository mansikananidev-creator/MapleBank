import {NavLink, useLocation, useNavigate} from 'react-router-dom'
import {
    Sidebar, SidebarContent, SidebarHeader, SidebarFooter,
    SidebarMenu, SidebarMenuItem, SidebarMenuButton, useSidebar, SidebarTrigger
} from '@/components/ui/sidebar'
import {useAuth} from "@/context/AuthContext.tsx";
import {ThemeToggle} from "@/components/ThemeToggle.tsx";
import {Button} from "@/components/ui/button.tsx";
import {ArrowLeftRight, Landmark, LayoutDashboard, LogOut, RefreshCw, ShieldAlert, ShieldCheck, User, Wallet} from "lucide-react";
import MapleLeafIcon from "@/components/MapleLeafIcon.tsx";

export default function AppSidebar() {
    const location = useLocation()
    const { user, logout } = useAuth()
    const navigate = useNavigate()
    const { setOpenMobile } = useSidebar()

    function handleLogout() {
        logout()
        navigate('/login')
    }

    function handleNavClick() {            // add here
        setOpenMobile(false)
    }

    return (
        <Sidebar collapsible="icon">
            <SidebarHeader>
                <div className="flex items-center gap-2 px-2 py-1 group-data-[collapsible=icon]:flex-col group-data-[collapsible=icon]:px-0">
                    <MapleLeafIcon size={20} className="text-primary shrink-0" />
                    <span className="font-bold text-lg group-data-[collapsible=icon]:hidden">Maple Bank</span>
                    <SidebarTrigger className="ml-auto group-data-[collapsible=icon]:ml-0" />
                </div>
            </SidebarHeader>
            <SidebarContent>
                <SidebarMenu className="group-data-[collapsible=icon]:items-center">
                    <SidebarMenuItem>
                        <SidebarMenuButton asChild isActive={location.pathname === '/dashboard'}>
                            <NavLink to="/dashboard" onClick={handleNavClick}>
                                <LayoutDashboard />
                                <span>Dashboard</span>
                            </NavLink>
                        </SidebarMenuButton>
                    </SidebarMenuItem>
                    <SidebarMenuItem>
                        <SidebarMenuButton asChild isActive={location.pathname === '/accounts'}>
                            <NavLink to="/accounts" onClick={handleNavClick}>
                                <Wallet />
                                <span>Accounts</span>
                            </NavLink>
                        </SidebarMenuButton>
                    </SidebarMenuItem>
                    <SidebarMenuItem>
                        <SidebarMenuButton asChild isActive={location.pathname === '/transactions'}>
                            <NavLink to="/transactions" onClick={handleNavClick}>
                                <ArrowLeftRight />
                                <span>Transactions</span>
                            </NavLink>
                        </SidebarMenuButton>
                    </SidebarMenuItem>
                    <SidebarMenuItem>
                        <SidebarMenuButton asChild isActive={location.pathname === '/loans'}>
                            <NavLink to="/loans" onClick={handleNavClick}>
                                <Landmark />
                                <span>Loans</span>
                            </NavLink>
                        </SidebarMenuButton>
                    </SidebarMenuItem>
                    <SidebarMenuItem>
                        <SidebarMenuButton asChild isActive={location.pathname === '/recurring-payments'}>
                            <NavLink to="/recurring-payments" onClick={handleNavClick}>
                                <RefreshCw />
                                <span>Recurring</span>
                            </NavLink>
                        </SidebarMenuButton>
                    </SidebarMenuItem>
                    <SidebarMenuItem>
                        <SidebarMenuButton asChild isActive={location.pathname === '/profile'}>
                            <NavLink to="/profile" onClick={handleNavClick}>
                                <User />
                                <span>Profile</span>
                            </NavLink>
                        </SidebarMenuButton>
                    </SidebarMenuItem>
                    {user?.role === 'ADMIN' && (
                        <SidebarMenuItem>
                            <SidebarMenuButton asChild isActive={location.pathname === '/admin/loans'}>
                                <NavLink to="/admin/loans" onClick={handleNavClick}>
                                    <ShieldCheck />
                                    <span>Loan review</span>
                                </NavLink>
                            </SidebarMenuButton>
                        </SidebarMenuItem>
                    )}
                    {user?.role === 'ADMIN' && (
                        <SidebarMenuItem>
                            <SidebarMenuButton asChild isActive={location.pathname === '/admin/compliance'}>
                                <NavLink to="/admin/compliance" onClick={handleNavClick}>
                                    <ShieldAlert />
                                    <span>Compliance</span>
                                </NavLink>
                            </SidebarMenuButton>
                        </SidebarMenuItem>
                    )}
                </SidebarMenu>
            </SidebarContent>
            <SidebarFooter>
                <div className="flex items-center justify-center py-2 border-t border-sidebar-border group-data-[collapsible=icon]:hidden">
                    <ThemeToggle />
                </div>
                <div className="flex items-center gap-2 px-2 pb-2 group-data-[collapsible=icon]:flex-col group-data-[collapsible=icon]:px-0">
                    <button
                        type="button"
                        onClick={() => { navigate('/profile'); handleNavClick() }}
                        className="flex items-center gap-2 flex-1 min-w-0 cursor-pointer"
                        aria-label="Go to profile"
                    >
                        <div className="flex items-center justify-center size-7 rounded-full bg-accent text-accent-foreground text-xs font-semibold shrink-0">
                            {user?.email?.[0]?.toUpperCase()}
                        </div>
                        <p className="text-xs text-muted-foreground truncate flex-1 group-data-[collapsible=icon]:hidden hover:text-foreground">{user?.email}</p>
                    </button>
                    <Button variant="ghost" size="icon" className="size-7" onClick={handleLogout}>
                        <LogOut />
                    </Button>
                </div>
            </SidebarFooter>
        </Sidebar>
    )
}