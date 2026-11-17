'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import {
  customerApi,
  NotificationResponse,
  PageResponse,
} from '@/lib/api/customer';
import { Button } from '@/components/ui/Button';
import { Skeleton } from '@/components/ui/Skeleton';
import { useToast } from '@/components/ui/Toast';
import {
  Bell,
  CheckCheck,
  Package,
  CreditCard,
  RefreshCw,
  ArrowRight,
  ArrowLeft,
  Info,
} from 'lucide-react';

function getNotificationIcon(type: string) {
  switch (type) {
    case 'ORDER':
      return <Package size={16} color="var(--wine)" />;
    case 'STORE_CREDIT':
      return <CreditCard size={16} color="var(--wine)" />;
    case 'RETURN':
      return <RefreshCw size={16} color="var(--wine)" />;
    default:
      return <Info size={16} color="var(--wine)" />;
  }
}

export default function NotificationsPage() {
  const { toast } = useToast();
  const [notificationsPage, setNotificationsPage] = useState<PageResponse<NotificationResponse> | null>(null);
  const [loading, setLoading] = useState(true);
  const [currentPage, setCurrentPage] = useState(0);
  const [markingAll, setMarkingAll] = useState(false);

  useEffect(() => {
    let isMounted = true;
    customerApi.getNotifications(currentPage, 15)
      .then((res) => {
        if (isMounted && res.data) setNotificationsPage(res.data);
      })
      .catch(() => {})
      .finally(() => {
        if (isMounted) setLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, [currentPage]);

  const handleMarkAsRead = async (id: string, currentlyRead: boolean) => {
    if (currentlyRead) return;
    try {
      await customerApi.markNotificationAsRead(id);
      setNotificationsPage((prev) => {
        if (!prev) return null;
        return {
          ...prev,
          content: prev.content.map((item) =>
            item.id === id ? { ...item, read: true } : item
          ),
        };
      });
    } catch {
      // Ignore
    }
  };

  const handleMarkAllRead = async () => {
    setMarkingAll(true);
    try {
      await customerApi.markAllNotificationsAsRead();
      toast({ type: 'success', title: 'Updated', message: 'All notifications marked as read.' });
      setNotificationsPage((prev) => {
        if (!prev) return null;
        return {
          ...prev,
          content: prev.content.map((item) => ({ ...item, read: true })),
        };
      });
    } catch {
      toast({ type: 'error', title: 'Error', message: 'Failed to mark all as read.' });
    } finally {
      setMarkingAll(false);
    }
  };

  const unreadCount = notificationsPage?.content.filter((n) => !n.read).length ?? 0;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '12px' }}>
        <div>
          <h1 className="font-serif" style={{ margin: 0, fontSize: '22px', fontWeight: 500 }}>
            Notifications
          </h1>
          <p style={{ margin: '4px 0 0', fontSize: '13px', color: 'var(--ink-muted)' }}>
            Real-time fulfillment updates, dispatch tracking, and store credit transactions.
          </p>
        </div>

        {unreadCount > 0 && (
          <Button
            variant="outline"
            size="sm"
            onClick={handleMarkAllRead}
            isLoading={markingAll}
          >
            <CheckCheck size={14} style={{ marginRight: '6px' }} />
            Mark all as read
          </Button>
        )}
      </div>

      {loading ? (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          <Skeleton height="70px" />
          <Skeleton height="70px" />
          <Skeleton height="70px" />
        </div>
      ) : !notificationsPage || notificationsPage.content.length === 0 ? (
        <div
          style={{
            padding: '48px 24px',
            textAlign: 'center',
            backgroundColor: '#ffffff',
            border: '1px solid var(--line)',
            borderRadius: 'var(--radius-sm)',
          }}
        >
          <Bell size={32} color="var(--ink-muted)" style={{ marginBottom: '12px' }} />
          <h3 style={{ margin: '0 0 6px', fontSize: '16px', fontWeight: 600 }}>Your inbox is quiet</h3>
          <p style={{ margin: 0, fontSize: '13px', color: 'var(--ink-muted)' }}>
            Order confirmations, dispatch notes, and credit ledger notifications will appear here.
          </p>
        </div>
      ) : (
        <div
          style={{
            backgroundColor: '#ffffff',
            border: '1px solid var(--line)',
            borderRadius: 'var(--radius-sm)',
            overflow: 'hidden',
          }}
        >
          {notificationsPage.content.map((item) => (
            <div
              key={item.id}
              onClick={() => handleMarkAsRead(item.id, item.read)}
              style={{
                display: 'flex',
                alignItems: 'flex-start',
                gap: '16px',
                padding: '16px 20px',
                borderBottom: '1px solid var(--line-light)',
                backgroundColor: item.read ? '#ffffff' : 'var(--wine-soft)',
                cursor: item.read ? 'default' : 'pointer',
                transition: 'background-color 0.15s ease',
              }}
            >
              <div
                style={{
                  width: '32px',
                  height: '32px',
                  borderRadius: '50%',
                  backgroundColor: '#ffffff',
                  border: '1px solid var(--line)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  flexShrink: 0,
                  marginTop: '2px',
                }}
              >
                {getNotificationIcon(item.type)}
              </div>

              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '4px' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <span style={{ fontSize: '14px', fontWeight: item.read ? 500 : 700, color: 'var(--ink)' }}>
                      {item.title}
                    </span>
                    {!item.read && (
                      <span
                        style={{
                          width: '6px',
                          height: '6px',
                          borderRadius: '50%',
                          backgroundColor: 'var(--wine)',
                          display: 'inline-block',
                        }}
                      />
                    )}
                  </div>
                  <span style={{ fontSize: '11px', color: 'var(--ink-muted)' }}>
                    {new Date(item.createdAt).toLocaleDateString('en-IN', {
                      day: 'numeric',
                      month: 'short',
                    })}
                  </span>
                </div>

                <p style={{ margin: 0, fontSize: '13px', color: 'var(--ink-secondary)', lineHeight: 1.4 }}>
                  {item.content}
                </p>

                {item.actionUrl && (
                  <Link
                    href={item.actionUrl}
                    style={{
                      display: 'inline-flex',
                      alignItems: 'center',
                      gap: '4px',
                      fontSize: '12px',
                      color: 'var(--wine)',
                      fontWeight: 500,
                      marginTop: '8px',
                      textDecoration: 'none',
                    }}
                  >
                    <span>View details</span>
                    <ArrowRight size={12} />
                  </Link>
                )}
              </div>
            </div>
          ))}

          {/* Pagination Controls */}
          {notificationsPage.totalPages > 1 && (
            <div style={{ display: 'flex', justifyContent: 'center', gap: '8px', padding: '16px' }}>
              <Button
                variant="outline"
                size="sm"
                disabled={currentPage === 0}
                onClick={() => setCurrentPage((p) => Math.max(0, p - 1))}
              >
                <ArrowLeft size={14} style={{ marginRight: '4px' }} />
                Previous
              </Button>
              <span style={{ display: 'flex', alignItems: 'center', fontSize: '13px', color: 'var(--ink-muted)', padding: '0 8px' }}>
                Page {currentPage + 1} of {notificationsPage.totalPages}
              </span>
              <Button
                variant="outline"
                size="sm"
                disabled={currentPage >= notificationsPage.totalPages - 1}
                onClick={() => setCurrentPage((p) => p + 1)}
              >
                Next
                <ArrowRight size={14} style={{ marginLeft: '4px' }} />
              </Button>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
