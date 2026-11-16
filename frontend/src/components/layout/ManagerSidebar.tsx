'use client';

import React from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { BrandLogo } from '../brand/BrandLogo';
import {
  ClipboardList,
  CheckSquare,
  PackageCheck,
  FileSpreadsheet,
  Truck,
  Shirt,
  Boxes,
  AlertTriangle,
  RotateCcw,
  IndianRupee,
  LifeBuoy,
  FileText,
  ArrowLeft,
} from 'lucide-react';

const MANAGER_NAV_GROUPS = [
  {
    title: 'Daily Operations',
    items: [
      { label: 'Operations Dashboard', href: '/manager', icon: ClipboardList },
      { label: 'Order Queue & Claims', href: '/manager/orders', icon: CheckSquare },
      { label: 'Picking & Packing', href: '/manager/fulfillment/picking', icon: PackageCheck },
      { label: 'Dispatch Manifests', href: '/manager/fulfillment/dispatch', icon: FileSpreadsheet },
      { label: 'Handover & Attempts', href: '/manager/fulfillment/delivery', icon: Truck },
    ],
  },
  {
    title: 'Inventory & Catalog Drafts',
    items: [
      { label: 'Stock Receiving & Levels', href: '/manager/inventory', icon: Boxes },
      { label: 'Low-Stock Alerts', href: '/manager/inventory/alerts', icon: AlertTriangle },
      { label: 'Product Drafts & Edits', href: '/manager/catalog/drafts', icon: Shirt },
    ],
  },
  {
    title: 'Returns, COD & Support',
    items: [
      { label: 'Inspection & Exchanges', href: '/manager/returns', icon: RotateCcw },
      { label: 'COD Settlement Batches', href: '/manager/settlements', icon: IndianRupee },
      { label: 'Support Ticket Inbox', href: '/manager/support', icon: LifeBuoy },
      { label: 'Operational Reports', href: '/manager/reports/operational', icon: FileText },
    ],
  },
];

export const ManagerSidebar: React.FC = () => {
  const pathname = usePathname();

  return (
    <aside className="dashboard-sidebar" aria-label="Manager Operations Navigation">
      <div className="sidebar-header">
        <BrandLogo variant="wordmark" width={140} height={44} href="/manager" />
        <div>
          <span className="sidebar-role-badge sidebar-role-manager">Operations Manager</span>
        </div>
      </div>

      <nav style={{ flex: 1 }}>
        {MANAGER_NAV_GROUPS.map((group) => (
          <div key={group.title} className="sidebar-nav-group">
            <div className="sidebar-group-title">{group.title}</div>
            {group.items.map((item) => {
              const Icon = item.icon;
              const isActive = pathname === item.href;
              return (
                <Link
                  key={item.href}
                  href={item.href}
                  className={`sidebar-nav-item ${isActive ? 'active' : ''}`}
                  aria-current={isActive ? 'page' : undefined}
                >
                  <Icon size={16} />
                  <span>{item.label}</span>
                </Link>
              );
            })}
          </div>
        ))}
      </nav>

      <div className="sidebar-footer">
        <Link
          href="/"
          className="sidebar-nav-item"
          style={{ color: 'var(--wine)', fontWeight: 500 }}
        >
          <ArrowLeft size={16} />
          <span>Back to Storefront</span>
        </Link>
      </div>
    </aside>
  );
};
