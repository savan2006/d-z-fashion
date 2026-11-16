import React from 'react';
import { AdminSidebar } from '@/components/layout/AdminSidebar';
import { AdminHeader } from '@/components/layout/AdminHeader';

export default function AdminLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="dashboard-shell">
      <AdminSidebar />
      <div className="dashboard-main">
        <AdminHeader />
        <main className="dashboard-content">{children}</main>
      </div>
    </div>
  );
}
