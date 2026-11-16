'use client';

import React from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { BrandLogo } from '../brand/BrandLogo';
import {
  LayoutDashboard,
  Shirt,
  DollarSign,
  Tags,
  Boxes,
  ShoppingBag,
  Percent,
  Truck,
  CheckCircle,
  RotateCcw,
  Users,
  LifeBuoy,
  ShieldAlert,
  ArrowLeft,
} from 'lucide-react';

const OWNER_NAV_GROUPS = [
  {
    title: 'Business & Insights',
    items: [
      { label: 'Overview & Analytics', href: '/admin', icon: LayoutDashboard },
      { label: 'Audit Trail', href: '/admin/audit', icon: ShieldAlert },
    ],
  },
  {
    title: 'Catalog & Merchandising',
    items: [
      { label: 'Catalog & Approvals', href: '/admin/catalog', icon: Shirt },
      { label: 'Price Management', href: '/admin/pricing', icon: DollarSign },
      { label: 'Categories & Tax', href: '/admin/categories', icon: Tags },
      { label: 'Coupons & Campaigns', href: '/admin/promotions', icon: Percent },
    ],
  },
  {
    title: 'Operations & Approvals',
    items: [
      { label: 'Orders & Reassignments', href: '/admin/orders', icon: ShoppingBag },
      { label: 'Inventory & Alerts', href: '/admin/inventory', icon: Boxes },
      { label: 'COD Settlement Approvals', href: '/admin/settlements', icon: CheckCircle },
      { label: 'Refund & Return Approvals', href: '/admin/returns', icon: RotateCcw },
      { label: 'COD PINs & Logistics', href: '/admin/logistics', icon: Truck },
    ],
  },
  {
    title: 'Administration',
    items: [
      { label: 'Staff & Accounts', href: '/admin/accounts', icon: Users },
      { label: 'Support Escalations', href: '/admin/support', icon: LifeBuoy },
    ],
  },
];

export const AdminSidebar: React.FC = () => {
  const pathname = usePathname();

  return (
    <aside className="dashboard-sidebar" aria-label="Owner Administration Navigation">
      <div className="sidebar-header">
        <BrandLogo variant="wordmark" width={140} height={44} href="/admin" />
        <div>
          <span className="sidebar-role-badge sidebar-role-owner">Owner Admin</span>
        </div>
      </div>

      <nav style={{ flex: 1 }}>
        {OWNER_NAV_GROUPS.map((group) => (
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
