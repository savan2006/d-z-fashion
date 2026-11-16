import React from 'react';
import { ManagerSidebar } from '@/components/layout/ManagerSidebar';
import { ManagerHeader } from '@/components/layout/ManagerHeader';

export default function ManagerLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="dashboard-shell">
      <ManagerSidebar />
      <div className="dashboard-main">
        <ManagerHeader />
        <main className="dashboard-content">{children}</main>
      </div>
    </div>
  );
}
