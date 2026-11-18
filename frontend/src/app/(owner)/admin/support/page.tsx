'use client';

import React, { useEffect, useState } from 'react';
import {
  adminApi,
  SupportTicketResponse,
  PageResponse,
} from '@/lib/api/admin';
import { Badge, BadgeVariant } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Dialog } from '@/components/ui/Dialog';
import { Skeleton } from '@/components/ui/Skeleton';
import { useToast } from '@/components/ui/Toast';

function getTicketStatusVariant(status: string): BadgeVariant {
  switch (status) {
    case 'RESOLVED':
    case 'CLOSED':
      return 'success';
    case 'IN_PROGRESS':
      return 'warning';
    case 'OPEN':
      return 'neutral';
    default:
      return 'neutral';
  }
}

export default function AdminSupportPage() {
  const { toast } = useToast();
  const [loading, setLoading] = useState(true);
  const [ticketsPage, setTicketsPage] = useState<PageResponse<SupportTicketResponse> | null>(null);

  // Detail Modal
  const [selectedTicket, setSelectedTicket] = useState<SupportTicketResponse | null>(null);
  const [newNote, setNewNote] = useState('');
  const [sendingNote, setSendingNote] = useState(false);

  // Escalate Modal
  const [escalateTarget, setEscalateTarget] = useState<SupportTicketResponse | null>(null);
  const [escalateReason, setEscalateReason] = useState('');
  const [escalating, setEscalateing] = useState(false);

  const loadTickets = () => {
    let isMounted = true;
    adminApi.getAllSupportTickets(undefined, undefined, undefined, 0, 20)
      .then((res) => {
        if (!isMounted || !res.data) return;
        setTicketsPage(res.data);
      })
      .catch(() => {})
      .finally(() => {
        if (isMounted) setLoading(false);
      });

    return () => {
      isMounted = false;
    };
  };

  useEffect(() => {
    loadTickets();
  }, []);

  const handleAddStaffNote = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedTicket || !newNote.trim()) return;

    setSendingNote(true);
    try {
      const res = await adminApi.addSupportStaffMessage(selectedTicket.id, newNote.trim(), true);
      if (res.data) setSelectedTicket(res.data);
      setNewNote('');
      toast({ type: 'success', title: 'Note Added', message: 'Internal staff note logged.' });
      loadTickets();
    } catch {
      toast({ type: 'error', title: 'Error', message: 'Failed to add staff note.' });
    } finally {
      setSendingNote(false);
    }
  };

  const handleEscalate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!escalateTarget || !escalateReason.trim()) return;

    setEscalateing(true);
    try {
      await adminApi.escalateSupportTicket(escalateTarget.id, escalateReason.trim());
      toast({ type: 'success', title: 'Ticket Escalated', message: 'Ticket prioritized as Owner Escalation.' });
      setEscalateTarget(null);
      setEscalateReason('');
      loadTickets();
    } catch {
      toast({ type: 'error', title: 'Error', message: 'Escalation failed.' });
    } finally {
      setEscalateing(false);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      <div>
        <h1 className="font-serif" style={{ margin: 0, fontSize: '22px', fontWeight: 500 }}>
          Support Escalations &amp; Customer Care
        </h1>
        <p style={{ margin: '4px 0 0', fontSize: '13px', color: 'var(--ink-muted)' }}>
          Manage customer inquiries across WhatsApp, Phone, and Web channels with hidden internal staff notes.
        </p>
      </div>

      {/* Tickets Table */}
      <div
        style={{
          backgroundColor: '#ffffff',
          border: '1px solid var(--line)',
          borderRadius: 'var(--radius-sm)',
          overflow: 'hidden',
        }}
      >
        <div style={{ padding: '16px 20px', borderBottom: '1px solid var(--line-light)' }}>
          <h2 style={{ margin: 0, fontSize: '15px', fontWeight: 600 }}>Active Support Queue</h2>
        </div>

        {loading ? (
          <div style={{ padding: '24px' }}>
            <Skeleton height="60px" style={{ marginBottom: '12px' }} />
            <Skeleton height="60px" />
          </div>
        ) : !ticketsPage || ticketsPage.content.length === 0 ? (
          <div style={{ padding: '48px 24px', textAlign: 'center', color: 'var(--ink-muted)', fontSize: '13px' }}>
            No support tickets in queue.
          </div>
        ) : (
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '13px' }}>
            <thead>
              <tr style={{ backgroundColor: 'var(--surface-subtle)', borderBottom: '1px solid var(--line)' }}>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Ticket #</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Channel</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Customer</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Subject</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Priority</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Status</th>
                <th style={{ padding: '12px 16px', fontWeight: 600, textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {ticketsPage.content.map((t) => (
                <tr key={t.id} style={{ borderBottom: '1px solid var(--line-light)' }}>
                  <td style={{ padding: '14px 16px', fontWeight: 600, color: 'var(--ink)' }}>
                    #{t.ticketNumber}
                  </td>
                  <td style={{ padding: '14px 16px' }}>
                    <Badge variant={t.channel === 'WHATSAPP' ? 'success' : 'neutral'}>
                      {t.channel}
                    </Badge>
                  </td>
                  <td style={{ padding: '14px 16px' }}>
                    <div style={{ fontWeight: 500 }}>{t.customerName}</div>
                    <div style={{ fontSize: '11px', color: 'var(--ink-muted)' }}>
                      {t.customerPhone || t.customerEmail}
                    </div>
                  </td>
                  <td style={{ padding: '14px 16px', maxWidth: '280px', color: 'var(--ink)' }}>
                    <div style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      {t.subject}
                    </div>
                  </td>
                  <td style={{ padding: '14px 16px' }}>
                    {t.escalated ? (
                      <Badge variant="error">ESCALATED</Badge>
                    ) : (
                      <Badge variant="neutral">{t.priority}</Badge>
                    )}
                  </td>
                  <td style={{ padding: '14px 16px' }}>
                    <Badge variant={getTicketStatusVariant(t.status)}>
                      {t.status.replace('_', ' ')}
                    </Badge>
                  </td>
                  <td style={{ padding: '14px 16px', textAlign: 'right' }}>
                    <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end' }}>
                      <Button variant="outline" size="sm" onClick={() => setSelectedTicket(t)}>
                        View Notes
                      </Button>
                      {!t.escalated && (
                        <Button variant="outline" size="sm" style={{ color: 'var(--wine)' }} onClick={() => setEscalateTarget(t)}>
                          Escalate
                        </Button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* Ticket Details & Staff Notes Dialog */}
      {selectedTicket && (
        <Dialog
          isOpen={true}
          onClose={() => setSelectedTicket(null)}
          title={`Support Ticket #${selectedTicket.ticketNumber}`}
          description={`Customer: ${selectedTicket.customerName} (${selectedTicket.channel} Channel)`}
          maxWidth="640px"
        >
          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <div style={{ padding: '12px 14px', backgroundColor: 'var(--surface-subtle)', borderRadius: 'var(--radius-sm)' }}>
              <strong>Subject:</strong> {selectedTicket.subject}
            </div>

            <div>
              <h3 style={{ margin: '0 0 8px', fontSize: '13px', fontWeight: 600 }}>Internal Staff Notes Log</h3>
              {selectedTicket.notes && selectedTicket.notes.length > 0 ? (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', maxHeight: '200px', overflowY: 'auto' }}>
                  {selectedTicket.notes.map((n) => (
                    <div
                      key={n.id}
                      style={{
                        padding: '10px 12px',
                        backgroundColor: n.isInternalStaffNote ? '#fef3c7' : '#ffffff',
                        border: '1px solid var(--line)',
                        borderRadius: 'var(--radius-sm)',
                        fontSize: '12px',
                      }}
                    >
                      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '2px', color: 'var(--ink-muted)' }}>
                        <span>{n.authorName} ({n.isInternalStaffNote ? 'Staff Internal' : 'Customer Message'})</span>
                        <span>{new Date(n.createdAt).toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' })}</span>
                      </div>
                      <div>{n.content}</div>
                    </div>
                  ))}
                </div>
              ) : (
                <p style={{ margin: 0, fontSize: '12px', color: 'var(--ink-muted)' }}>No staff notes recorded yet.</p>
              )}
            </div>

            {/* Add note input */}
            <form onSubmit={handleAddStaffNote} style={{ display: 'flex', gap: '8px' }}>
              <input
                type="text"
                className="form-input"
                placeholder="Add internal staff note (hidden from customer)..."
                value={newNote}
                onChange={(e) => setNewNote(e.target.value)}
                style={{ flex: 1 }}
                required
              />
              <Button type="submit" variant="primary" isLoading={sendingNote}>
                Log Note
              </Button>
            </form>
          </div>
        </Dialog>
      )}

      {/* Escalate Dialog */}
      {escalateTarget && (
        <Dialog
          isOpen={true}
          onClose={() => setEscalateTarget(null)}
          title={`Escalate Ticket #${escalateTarget.ticketNumber}`}
          description="Flag this support inquiry for high-priority Owner executive attention."
        >
          <form onSubmit={handleEscalate} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <div>
              <label className="form-label" style={{ display: 'block', marginBottom: '6px' }}>
                Escalation Justification <span style={{ color: 'var(--wine)' }}>*</span>
              </label>
              <textarea
                className="form-input"
                rows={3}
                placeholder="e.g. VIP client wedding deadline issue or high-value zari defect..."
                value={escalateReason}
                onChange={(e) => setEscalateReason(e.target.value)}
                required
                style={{ width: '100%', resize: 'vertical' }}
              />
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '8px' }}>
              <Button type="button" variant="outline" onClick={() => setEscalateTarget(null)} disabled={escalating}>
                Cancel
              </Button>
              <Button type="submit" variant="primary" isLoading={escalating}>
                Escalate Ticket
              </Button>
            </div>
          </form>
        </Dialog>
      )}
    </div>
  );
}
