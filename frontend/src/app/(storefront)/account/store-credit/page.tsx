'use client';

import React, { useEffect, useState } from 'react';
import {
  customerApi,
  StoreCreditBalanceResponse,
  StoreCreditEntryResponse,
  PageResponse,
} from '@/lib/api/customer';
import { Button } from '@/components/ui/Button';
import { Skeleton } from '@/components/ui/Skeleton';
import {
  CreditCard,
  ShieldCheck,
  ArrowUpRight,
  ArrowDownLeft,
  ArrowRight,
  ArrowLeft,
  Sparkles,
} from 'lucide-react';

export default function StoreCreditPage() {
  const [balance, setBalance] = useState<StoreCreditBalanceResponse | null>(null);
  const [ledgerPage, setLedgerPage] = useState<PageResponse<StoreCreditEntryResponse> | null>(null);
  const [loading, setLoading] = useState(true);
  const [currentPage, setCurrentPage] = useState(0);

  useEffect(() => {
    let isMounted = true;
    Promise.all([
      customerApi.getStoreCreditBalance().catch(() => null),
      customerApi.getStoreCreditLedger(currentPage, 15).catch(() => null),
    ]).then(([balRes, ledgRes]) => {
      if (!isMounted) return;
      if (balRes && balRes.data) setBalance(balRes.data);
      if (ledgRes && ledgRes.data) setLedgerPage(ledgRes.data);
      setLoading(false);
    });

    return () => {
      isMounted = false;
    };
  }, [currentPage]);

  if (loading) {
    return (
      <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
        <Skeleton height="160px" />
        <Skeleton height="300px" />
      </div>
    );
  }

  const currentBal = balance?.currentBalance ?? 0;
  const totalIssued = balance?.totalIssued ?? 0;
  const totalRedeemed = balance?.totalRedeemed ?? 0;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      {/* Page Header */}
      <div>
        <h1 className="font-serif" style={{ margin: 0, fontSize: '22px', fontWeight: 500 }}>
          Store Credit Wallet
        </h1>
        <p style={{ margin: '4px 0 0', fontSize: '13px', color: 'var(--ink-muted)' }}>
          Seamless non-expiring balance applicable towards any future luxury ethnic order.
        </p>
      </div>

      {/* Hero Wallet Card */}
      <div
        style={{
          background: 'linear-gradient(135deg, #4a1320 0%, #6b1d2f 60%, #300c14 100%)',
          color: '#ffffff',
          borderRadius: 'var(--radius-md)',
          padding: '32px',
          boxShadow: 'var(--shadow-md)',
          position: 'relative',
          overflow: 'hidden',
        }}
      >
        <div style={{ position: 'relative', zIndex: 2 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <CreditCard size={20} color="#e5c07b" />
              <span style={{ fontSize: '12px', letterSpacing: '0.1em', textTransform: 'uppercase', color: 'rgba(255, 255, 255, 0.8)' }}>
                D&amp;Z Fashion Ledger
              </span>
            </div>
            <div
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '6px',
                backgroundColor: 'rgba(255, 255, 255, 0.15)',
                border: '1px solid rgba(255, 255, 255, 0.25)',
                padding: '4px 10px',
                borderRadius: '16px',
                fontSize: '11px',
                fontWeight: 600,
                color: '#ffffff',
              }}
            >
              <Sparkles size={12} color="#e5c07b" />
              <span>NEVER EXPIRES</span>
            </div>
          </div>

          <div style={{ marginBottom: '24px' }}>
            <span style={{ fontSize: '13px', color: 'rgba(255, 255, 255, 0.75)', display: 'block', marginBottom: '4px' }}>
              Available Balance
            </span>
            <div className="font-serif" style={{ fontSize: '38px', fontWeight: 400, letterSpacing: '-0.02em', color: '#ffffff' }}>
              ₹{currentBal.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
            </div>
          </div>

          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
              gap: '16px',
              borderTop: '1px solid rgba(255, 255, 255, 0.15)',
              paddingTop: '16px',
            }}
          >
            <div>
              <span style={{ fontSize: '11px', color: 'rgba(255, 255, 255, 0.65)', display: 'block' }}>
                Total Lifetime Issued
              </span>
              <span style={{ fontSize: '14px', fontWeight: 600 }}>
                ₹{totalIssued.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
              </span>
            </div>

            <div>
              <span style={{ fontSize: '11px', color: 'rgba(255, 255, 255, 0.65)', display: 'block' }}>
                Total Redeemed at Checkout
              </span>
              <span style={{ fontSize: '14px', fontWeight: 600 }}>
                ₹{totalRedeemed.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
              </span>
            </div>

            <div>
              <span style={{ fontSize: '11px', color: 'rgba(255, 255, 255, 0.65)', display: 'block' }}>
                Redemption Rule
              </span>
              <span style={{ fontSize: '13px', color: '#e5c07b' }}>
                100% redeemable on any order
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* Policy Card */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: '12px',
          padding: '16px 20px',
          backgroundColor: '#ffffff',
          border: '1px solid var(--line)',
          borderRadius: 'var(--radius-sm)',
          fontSize: '13px',
          color: 'var(--ink-secondary)',
        }}
      >
        <ShieldCheck size={20} color="var(--wine)" />
        <div>
          Store credit is automatically deposited when an order is cancelled or when a return resolution is accepted. You can apply all or part of your balance during checkout.
        </div>
      </div>

      {/* Ledger History */}
      <div
        style={{
          backgroundColor: '#ffffff',
          border: '1px solid var(--line)',
          borderRadius: 'var(--radius-sm)',
          padding: '24px',
        }}
      >
        <h2 style={{ margin: '0 0 16px', fontSize: '16px', fontWeight: 600 }}>
          Transaction Activity Log
        </h2>

        {!ledgerPage || ledgerPage.content.length === 0 ? (
          <div style={{ padding: '32px 0', textAlign: 'center', color: 'var(--ink-muted)', fontSize: '13px' }}>
            No transaction entries in your credit ledger.
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column' }}>
            {ledgerPage.content.map((entry) => {
              const isCredit = entry.entryType === 'CREDIT';
              return (
                <div
                  key={entry.id}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    padding: '16px 0',
                    borderBottom: '1px solid var(--line-light)',
                    gap: '16px',
                    flexWrap: 'wrap',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                    <div
                      style={{
                        width: '36px',
                        height: '36px',
                        borderRadius: '50%',
                        backgroundColor: isCredit ? 'var(--success-soft)' : 'var(--error-soft)',
                        color: isCredit ? 'var(--success)' : 'var(--error)',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                      }}
                    >
                      {isCredit ? <ArrowDownLeft size={18} /> : <ArrowUpRight size={18} />}
                    </div>

                    <div>
                      <div style={{ fontWeight: 600, fontSize: '13px', color: 'var(--ink)' }}>
                        {entry.reason}
                      </div>
                      <div style={{ fontSize: '12px', color: 'var(--ink-muted)', marginTop: '2px' }}>
                        {new Date(entry.createdAt).toLocaleString('en-IN', {
                          dateStyle: 'medium',
                          timeStyle: 'short',
                        })}
                        {entry.referenceId && ` \u2022 Ref: ${entry.referenceId}`}
                      </div>
                    </div>
                  </div>

                  <div style={{ textAlign: 'right' }}>
                    <div
                      style={{
                        fontWeight: 600,
                        fontSize: '14px',
                        color: isCredit ? 'var(--success)' : 'var(--ink)',
                      }}
                    >
                      {isCredit ? '+' : '-'}₹{entry.amount.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                    </div>
                    <span style={{ fontSize: '11px', color: 'var(--ink-muted)' }}>
                      Bal: ₹{entry.balanceAfter.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                    </span>
                  </div>
                </div>
              );
            })}

            {/* Pagination Controls */}
            {ledgerPage.totalPages > 1 && (
              <div style={{ display: 'flex', justifyContent: 'center', gap: '8px', marginTop: '20px' }}>
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
                  Page {currentPage + 1} of {ledgerPage.totalPages}
                </span>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={currentPage >= ledgerPage.totalPages - 1}
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
    </div>
  );
}
