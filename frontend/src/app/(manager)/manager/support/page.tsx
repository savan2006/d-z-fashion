'use client';

import React, { useEffect, useState, useTransition, useCallback } from 'react';
import {
  LifeBuoy,
  Plus,
  MessageSquare,
  AlertCircle,
  CheckCircle2,
  ShieldAlert,
  Search,
} from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Skeleton } from '@/components/ui/Skeleton';
import {
  adminApi,
  SupportTicketResponse,
  ManualSupportIntakeRequest,
} from '@/lib/api/admin';

export default function ManagerSupportPage() {
  const [tickets, setTickets] = useState<SupportTicketResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error' | 'warning'; text: string } | null>(null);

  // Manual Intake Modal State
  const [isIntakingManual, setIsIntakingManual] = useState(false);
  const [customerName, setCustomerName] = useState('');
  const [customerPhone, setCustomerPhone] = useState('');
  const [channel, setChannel] = useState<'WHATSAPP' | 'PHONE'>('WHATSAPP');
  const [subject, setSubject] = useState('');
  const [description, setDescription] = useState('');
  const [orderNumber, setOrderNumber] = useState('');
  const [priority, setPriority] = useState<'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT'>('MEDIUM');
  const [isSubmittingIntake, startIntakeTransition] = useTransition();

  // Internal Staff Note Modal State
  const [activeTicket, setActiveTicket] = useState<SupportTicketResponse | null>(null);
  const [staffNoteContent, setStaffNoteContent] = useState('');
  const [isInternalStaffNote, setIsInternalStaffNote] = useState(true);
  const [isSubmittingNote, startNoteTransition] = useTransition();

  // Escalation Modal
  const [escalatingTicket, setEscalatingTicket] = useState<SupportTicketResponse | null>(null);
  const [escalationReason, setEscalationReason] = useState('');
  const [isSubmittingEscalation, startEscalationTransition] = useTransition();

  const loadTickets = useCallback(() => {
    adminApi
      .getAllSupportTickets(statusFilter === 'ALL' ? undefined : statusFilter, undefined, undefined, 0, 50)
      .then((res) => {
        if (res.data?.content) {
          setTickets(res.data.content);
        } else {
          // Fallback mock tickets
          setTickets([
            {
              id: 'ticket-101',
              ticketNumber: 'TKT-20261118-01',
              customerName: 'Aarti Desai',
              customerEmail: 'aarti.desai@example.com',
              customerPhone: '9825012345',
              channel: 'WHATSAPP',
              subject: 'Delivery address update request before dispatch',
              status: 'OPEN',
              priority: 'HIGH',
              escalated: false,
              assignedToStaffName: 'Operations Staff',
              notes: [
                {
                  id: 'note-1',
                  authorName: 'Aarti Desai',
                  isInternalStaffNote: false,
                  content: 'Kindly update delivery flat number to B-402 instead of B-401.',
                  createdAt: '2026-11-18T06:00:00Z',
                },
              ],
              createdAt: '2026-11-18T06:00:00Z',
            },
            {
              id: 'ticket-102',
              ticketNumber: 'TKT-20261117-02',
              customerName: 'Pooja Mehta',
              customerEmail: 'pooja.mehta@example.com',
              customerPhone: '9898054321',
              channel: 'PHONE',
              subject: 'Return status inquiry for silk saree',
              status: 'IN_PROGRESS',
              priority: 'MEDIUM',
              escalated: false,
              assignedToStaffName: 'Operations Staff',
              notes: [
                {
                  id: 'note-2',
                  authorName: 'Staff Note',
                  isInternalStaffNote: true,
                  content: 'Parcel arrived at Surat warehouse; physical inspection pending on rack 2.',
                  createdAt: '2026-11-17T15:30:00Z',
                },
              ],
              createdAt: '2026-11-17T12:00:00Z',
            },
          ]);
        }
      })
      .finally(() => setLoading(false));
  }, [statusFilter]);

  useEffect(() => {
    loadTickets();
  }, [loadTickets]);

  const handleManualIntakeSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!customerName.trim() || !customerPhone.trim() || !subject.trim() || !description.trim()) return;

    startIntakeTransition(async () => {
      try {
        const payload: ManualSupportIntakeRequest = {
          customerName: customerName.trim(),
          customerPhone: customerPhone.trim(),
          channel,
          subject: subject.trim(),
          description: description.trim(),
          orderNumber: orderNumber.trim() || undefined,
          priority,
        };

        const res = await adminApi.recordManualSupportIntake(payload);

        setFeedback({
          type: 'success',
          text: `Inquiry ticket ${res.data?.ticketNumber || 'TKT-NEW'} logged from ${channel} channel.`,
        });

        setIsIntakingManual(false);
        setCustomerName('');
        setCustomerPhone('');
        setSubject('');
        setDescription('');
        setOrderNumber('');
        loadTickets();
      } catch (err: unknown) {
        const msg = err instanceof Error ? err.message : 'Failed to record manual support intake.';
        setFeedback({
          type: 'error',
          text: msg,
        });
      }
    });
  };

  const handleAddStaffNoteSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!activeTicket || !staffNoteContent.trim()) return;

    startNoteTransition(async () => {
      try {
        await adminApi.addSupportStaffMessage(activeTicket.id, staffNoteContent.trim(), isInternalStaffNote);

        setFeedback({
          type: 'success',
          text: `Staff note appended to ticket #${activeTicket.ticketNumber}.`,
        });

        setActiveTicket(null);
        setStaffNoteContent('');
        loadTickets();
      } catch (err: unknown) {
        const msg = err instanceof Error ? err.message : 'Failed to append note.';
        setFeedback({
          type: 'error',
          text: msg,
        });
      }
    });
  };

  const handleEscalateSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!escalatingTicket || !escalationReason.trim()) return;

    startEscalationTransition(async () => {
      try {
        await adminApi.escalateSupportTicket(escalatingTicket.id, escalationReason.trim());

        setFeedback({
          type: 'warning',
          text: `Ticket #${escalatingTicket.ticketNumber} escalated to Owner administration.`,
        });

        setEscalatingTicket(null);
        setEscalationReason('');
        loadTickets();
      } catch (err: unknown) {
        const msg = err instanceof Error ? err.message : 'Failed to escalate ticket.';
        setFeedback({
          type: 'error',
          text: msg,
        });
      }
    });
  };

  const handleUpdateStatus = async (ticketId: string, newStatus: 'OPEN' | 'IN_PROGRESS' | 'WAITING_ON_CUSTOMER' | 'RESOLVED' | 'CLOSED') => {
    try {
      await adminApi.updateSupportTicketStatus(ticketId, newStatus);
      setFeedback({
        type: 'success',
        text: `Ticket status updated to ${newStatus}.`,
      });
      loadTickets();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to update ticket status.';
      setFeedback({
        type: 'error',
        text: msg,
      });
    }
  };

  const filteredTickets = tickets.filter(
    (t) =>
      t.ticketNumber.toLowerCase().includes(searchQuery.toLowerCase()) ||
      t.customerName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      t.subject.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <div>
      {/* Header */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: '24px',
          flexWrap: 'wrap',
          gap: '12px',
        }}
      >
        <div>
          <h1 className="font-serif" style={{ fontSize: '24px', fontWeight: 500, margin: '0 0 6px', color: 'var(--ink)' }}>
            Customer Support Intake & Ticketing
          </h1>
          <p style={{ margin: 0, fontSize: '13px', color: 'var(--ink-muted)' }}>
            Log inbound customer inquiries from WhatsApp and phone, attach internal warehouse notes, and escalate to Owner when needed.
          </p>
        </div>
        <Button variant="primary" size="sm" onClick={() => setIsIntakingManual(true)}>
          <Plus size={14} style={{ marginRight: '6px' }} />
          Log WhatsApp / Phone Inquiry
        </Button>
      </div>

      {/* Feedback Banner */}
      {feedback && (
        <div
          role="status"
          style={{
            padding: '12px 16px',
            borderRadius: 'var(--radius-sm)',
            marginBottom: '20px',
            fontSize: '13px',
            backgroundColor:
              feedback.type === 'success'
                ? 'var(--color-success-bg)'
                : feedback.type === 'warning'
                ? '#fffbeb'
                : 'var(--error-soft)',
            color:
              feedback.type === 'success'
                ? 'var(--color-success)'
                : feedback.type === 'warning'
                ? '#b45309'
                : 'var(--error)',
            border: `1px solid ${
              feedback.type === 'success' ? '#bbf7d0' : feedback.type === 'warning' ? '#fde68a' : '#fecaca'
            }`,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            {feedback.type === 'success' && <CheckCircle2 size={16} />}
            {feedback.type === 'warning' && <AlertCircle size={16} />}
            {feedback.type === 'error' && <ShieldAlert size={16} />}
            <span>{feedback.text}</span>
          </div>
          <button
            onClick={() => setFeedback(null)}
            style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'inherit' }}
          >
            &times;
          </button>
        </div>
      )}

      {/* Filter and Search Bar */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          gap: '12px',
          marginBottom: '20px',
          flexWrap: 'wrap',
        }}
      >
        <div style={{ display: 'flex', gap: '8px' }}>
          {(['ALL', 'OPEN', 'IN_PROGRESS', 'RESOLVED'] as const).map((status) => (
            <button
              key={status}
              onClick={() => setStatusFilter(status)}
              style={{
                padding: '6px 14px',
                fontSize: '13px',
                fontWeight: statusFilter === status ? 600 : 400,
                backgroundColor: statusFilter === status ? 'var(--ink)' : '#ffffff',
                color: statusFilter === status ? '#ffffff' : 'var(--ink-muted)',
                border: '1px solid var(--line)',
                borderRadius: 'var(--radius-sm)',
                cursor: 'pointer',
              }}
            >
              {status === 'ALL' ? 'All Tickets' : status.replace('_', ' ')}
            </button>
          ))}
        </div>

        <div style={{ position: 'relative', minWidth: '260px' }}>
          <Search
            size={14}
            style={{ position: 'absolute', left: '10px', top: '50%', transform: 'translateY(-50%)', color: 'var(--ink-muted)' }}
          />
          <input
            type="text"
            placeholder="Search ticket #, customer, or subject..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            style={{
              width: '100%',
              padding: '6px 10px 6px 32px',
              fontSize: '13px',
              border: '1px solid var(--line)',
              borderRadius: 'var(--radius-sm)',
            }}
          />
        </div>
      </div>

      {/* Tickets Table */}
      {loading ? (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          <Skeleton height="56px" />
          <Skeleton height="56px" />
        </div>
      ) : filteredTickets.length === 0 ? (
        <div
          style={{
            backgroundColor: '#ffffff',
            border: '1px solid var(--line)',
            borderRadius: 'var(--radius-sm)',
            padding: '48px 24px',
            textAlign: 'center',
            color: 'var(--ink-muted)',
          }}
        >
          <LifeBuoy size={36} style={{ margin: '0 auto 12px', opacity: 0.4 }} />
          <p style={{ margin: '0 0 6px', fontWeight: 500, fontSize: '15px', color: 'var(--ink)' }}>
            No support tickets match selected criteria
          </p>
          <p style={{ margin: 0, fontSize: '13px' }}>
            Check back for incoming customer messages.
          </p>
        </div>
      ) : (
        <div
          style={{
            backgroundColor: '#ffffff',
            border: '1px solid var(--line)',
            borderRadius: 'var(--radius-sm)',
            overflowX: 'auto',
          }}
        >
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '13px' }}>
            <thead>
              <tr style={{ backgroundColor: 'var(--paper-warm)', borderBottom: '1px solid var(--line)' }}>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Ticket #</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Channel & Customer</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Subject</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Priority</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Status</th>
                <th style={{ padding: '12px 16px', fontWeight: 600, textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredTickets.map((t) => (
                <tr key={t.id} style={{ borderBottom: '1px solid var(--line)' }}>
                  <td style={{ padding: '12px 16px', fontWeight: 600 }}>
                    <span className="font-mono">{t.ticketNumber}</span>
                    <div style={{ fontSize: '11px', color: 'var(--ink-muted)', fontWeight: 400 }}>
                      {new Date(t.createdAt).toLocaleDateString('en-IN', {
                        day: '2-digit',
                        month: 'short',
                        hour: '2-digit',
                        minute: '2-digit',
                      })}
                    </div>
                  </td>
                  <td style={{ padding: '12px 16px' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                      <Badge variant="outline" size="sm">
                        {t.channel}
                      </Badge>
                      <span style={{ fontWeight: 500 }}>{t.customerName}</span>
                    </div>
                    {t.customerPhone && (
                      <div style={{ fontSize: '11px', color: 'var(--ink-muted)' }}>{t.customerPhone}</div>
                    )}
                  </td>
                  <td style={{ padding: '12px 16px' }}>
                    <div style={{ fontWeight: 500 }}>{t.subject}</div>
                    {t.escalated && (
                      <Badge variant="danger" size="sm" style={{ marginTop: '4px' }}>
                        Escalated to Owner
                      </Badge>
                    )}
                  </td>
                  <td style={{ padding: '12px 16px' }}>
                    <Badge
                      variant={
                        t.priority === 'URGENT'
                          ? 'danger'
                          : t.priority === 'HIGH'
                          ? 'warning'
                          : 'secondary'
                      }
                      size="sm"
                    >
                      {t.priority}
                    </Badge>
                  </td>
                  <td style={{ padding: '12px 16px' }}>
                    <Badge
                      variant={
                        t.status === 'RESOLVED' || t.status === 'CLOSED'
                          ? 'success'
                          : t.status === 'IN_PROGRESS'
                          ? 'secondary'
                          : 'warning'
                      }
                      size="sm"
                    >
                      {t.status}
                    </Badge>
                  </td>
                  <td style={{ padding: '12px 16px', textAlign: 'right' }}>
                    <div style={{ display: 'flex', gap: '6px', justifyContent: 'flex-end', flexWrap: 'wrap' }}>
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => {
                          setActiveTicket(t);
                          setStaffNoteContent('');
                          setIsInternalStaffNote(true);
                        }}
                      >
                        <MessageSquare size={13} style={{ marginRight: '4px' }} />
                        Staff Note
                      </Button>

                      {t.status !== 'RESOLVED' && (
                        <Button
                          variant="ghost"
                          size="sm"
                          style={{ color: 'var(--color-success)' }}
                          onClick={() => handleUpdateStatus(t.id, 'RESOLVED')}
                        >
                          Resolve
                        </Button>
                      )}

                      {!t.escalated && t.status !== 'CLOSED' && (
                        <Button
                          variant="ghost"
                          size="sm"
                          style={{ color: 'var(--wine)' }}
                          onClick={() => {
                            setEscalatingTicket(t);
                            setEscalationReason('');
                          }}
                        >
                          Escalate
                        </Button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Log WhatsApp / Phone Inquiry Modal */}
      {isIntakingManual && (
        <div
          role="dialog"
          aria-modal="true"
          style={{
            position: 'fixed',
            inset: 0,
            backgroundColor: 'rgba(0,0,0,0.5)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: '20px',
            zIndex: 100,
          }}
        >
          <div
            style={{
              backgroundColor: '#ffffff',
              borderRadius: 'var(--radius-sm)',
              maxWidth: '520px',
              width: '100%',
              padding: '28px',
              border: '1px solid var(--line)',
              maxHeight: '90vh',
              overflowY: 'auto',
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <h2 className="font-serif" style={{ margin: 0, fontSize: '18px', fontWeight: 500 }}>
                Log Inbound Inquiry (WhatsApp / Phone)
              </h2>
              <button
                onClick={() => setIsIntakingManual(false)}
                style={{ background: 'none', border: 'none', cursor: 'pointer', fontSize: '18px' }}
              >
                &times;
              </button>
            </div>

            <form onSubmit={handleManualIntakeSubmit}>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px', marginBottom: '12px' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, marginBottom: '4px' }}>
                    Channel *
                  </label>
                  <select
                    value={channel}
                    onChange={(e) => setChannel(e.target.value as 'WHATSAPP' | 'PHONE')}
                    style={{
                      width: '100%',
                      padding: '8px 12px',
                      fontSize: '13px',
                      border: '1px solid var(--line)',
                      borderRadius: 'var(--radius-sm)',
                      backgroundColor: '#ffffff',
                    }}
                  >
                    <option value="WHATSAPP">WhatsApp Business</option>
                    <option value="PHONE">Phone Helpline Call</option>
                  </select>
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, marginBottom: '4px' }}>
                    Priority *
                  </label>
                  <select
                    value={priority}
                    onChange={(e) => setPriority(e.target.value as 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT')}
                    style={{
                      width: '100%',
                      padding: '8px 12px',
                      fontSize: '13px',
                      border: '1px solid var(--line)',
                      borderRadius: 'var(--radius-sm)',
                      backgroundColor: '#ffffff',
                    }}
                  >
                    <option value="LOW">Low</option>
                    <option value="MEDIUM">Medium</option>
                    <option value="HIGH">High</option>
                    <option value="URGENT">Urgent (Pre-dispatch)</option>
                  </select>
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px', marginBottom: '12px' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, marginBottom: '4px' }}>
                    Customer Name *
                  </label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. Meera Patel"
                    value={customerName}
                    onChange={(e) => setCustomerName(e.target.value)}
                    style={{
                      width: '100%',
                      padding: '8px 12px',
                      fontSize: '13px',
                      border: '1px solid var(--line)',
                      borderRadius: 'var(--radius-sm)',
                    }}
                  />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, marginBottom: '4px' }}>
                    Customer Phone *
                  </label>
                  <input
                    type="tel"
                    required
                    placeholder="e.g. 9825012345"
                    value={customerPhone}
                    onChange={(e) => setCustomerPhone(e.target.value)}
                    style={{
                      width: '100%',
                      padding: '8px 12px',
                      fontSize: '13px',
                      border: '1px solid var(--line)',
                      borderRadius: 'var(--radius-sm)',
                    }}
                  />
                </div>
              </div>

              <div style={{ marginBottom: '12px' }}>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, marginBottom: '4px' }}>
                  Related Order # (Optional)
                </label>
                <input
                  type="text"
                  placeholder="e.g. ORD-20261118-001"
                  value={orderNumber}
                  onChange={(e) => setOrderNumber(e.target.value)}
                  style={{
                    width: '100%',
                    padding: '8px 12px',
                    fontSize: '13px',
                    border: '1px solid var(--line)',
                    borderRadius: 'var(--radius-sm)',
                  }}
                />
              </div>

              <div style={{ marginBottom: '12px' }}>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, marginBottom: '4px' }}>
                  Subject *
                </label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Request to delay dispatch date to Friday"
                  value={subject}
                  onChange={(e) => setSubject(e.target.value)}
                  style={{
                    width: '100%',
                    padding: '8px 12px',
                    fontSize: '13px',
                    border: '1px solid var(--line)',
                    borderRadius: 'var(--radius-sm)',
                  }}
                />
              </div>

              <div style={{ marginBottom: '20px' }}>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, marginBottom: '4px' }}>
                  Inquiry Description *
                </label>
                <textarea
                  rows={3}
                  required
                  placeholder="Summary of customer request..."
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  style={{
                    width: '100%',
                    padding: '8px 12px',
                    fontSize: '13px',
                    border: '1px solid var(--line)',
                    borderRadius: 'var(--radius-sm)',
                  }}
                />
              </div>

              <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end' }}>
                <Button variant="outline" type="button" onClick={() => setIsIntakingManual(false)}>
                  Cancel
                </Button>
                <Button variant="primary" type="submit" isLoading={isSubmittingIntake}>
                  Log Inquiry Ticket
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Internal Staff Note Modal */}
      {activeTicket && (
        <div
          role="dialog"
          aria-modal="true"
          style={{
            position: 'fixed',
            inset: 0,
            backgroundColor: 'rgba(0,0,0,0.5)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: '20px',
            zIndex: 100,
          }}
        >
          <div
            style={{
              backgroundColor: '#ffffff',
              borderRadius: 'var(--radius-sm)',
              maxWidth: '520px',
              width: '100%',
              padding: '28px',
              border: '1px solid var(--line)',
              maxHeight: '90vh',
              overflowY: 'auto',
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <h2 className="font-serif" style={{ margin: 0, fontSize: '18px', fontWeight: 500 }}>
                Ticket #{activeTicket.ticketNumber} — Notes
              </h2>
              <button
                onClick={() => setActiveTicket(null)}
                style={{ background: 'none', border: 'none', cursor: 'pointer', fontSize: '18px' }}
              >
                &times;
              </button>
            </div>

            {/* Conversation Log */}
            <div
              style={{
                backgroundColor: 'var(--paper-warm)',
                padding: '14px',
                borderRadius: 'var(--radius-sm)',
                marginBottom: '16px',
                maxHeight: '200px',
                overflowY: 'auto',
                display: 'flex',
                flexDirection: 'column',
                gap: '8px',
              }}
            >
              {activeTicket.notes.length === 0 ? (
                <div style={{ fontSize: '12px', color: 'var(--ink-muted)', textAlign: 'center' }}>
                  No messages or notes recorded yet.
                </div>
              ) : (
                activeTicket.notes.map((n) => (
                  <div
                    key={n.id}
                    style={{
                      padding: '8px 12px',
                      backgroundColor: n.isInternalStaffNote ? '#fef3c7' : '#ffffff',
                      border: '1px solid var(--line)',
                      borderRadius: 'var(--radius-sm)',
                      fontSize: '12px',
                    }}
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '2px', fontWeight: 600 }}>
                      <span style={{ color: n.isInternalStaffNote ? '#b45309' : 'var(--ink)' }}>
                        {n.authorName} {n.isInternalStaffNote && '(Internal Staff Note)'}
                      </span>
                      <span style={{ fontSize: '10px', color: 'var(--ink-muted)' }}>
                        {new Date(n.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                      </span>
                    </div>
                    <div>{n.content}</div>
                  </div>
                ))
              )}
            </div>

            <form onSubmit={handleAddStaffNoteSubmit}>
              <div style={{ marginBottom: '14px' }}>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, marginBottom: '4px' }}>
                  Add Note
                </label>
                <textarea
                  rows={3}
                  required
                  placeholder="Record warehouse picking details, customer call outcome, or coordination note..."
                  value={staffNoteContent}
                  onChange={(e) => setStaffNoteContent(e.target.value)}
                  style={{
                    width: '100%',
                    padding: '8px 12px',
                    fontSize: '13px',
                    border: '1px solid var(--line)',
                    borderRadius: 'var(--radius-sm)',
                  }}
                />
              </div>

              <div style={{ marginBottom: '20px' }}>
                <label style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '12px', cursor: 'pointer' }}>
                  <input
                    type="checkbox"
                    checked={isInternalStaffNote}
                    onChange={(e) => setIsInternalStaffNote(e.target.checked)}
                  />
                  Mark as Internal Staff Note (Hidden from Customer)
                </label>
              </div>

              <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end' }}>
                <Button variant="outline" type="button" onClick={() => setActiveTicket(null)}>
                  Cancel
                </Button>
                <Button variant="primary" type="submit" isLoading={isSubmittingNote}>
                  Post Note
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Escalate Modal */}
      {escalatingTicket && (
        <div
          role="dialog"
          aria-modal="true"
          style={{
            position: 'fixed',
            inset: 0,
            backgroundColor: 'rgba(0,0,0,0.5)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: '20px',
            zIndex: 100,
          }}
        >
          <div
            style={{
              backgroundColor: '#ffffff',
              borderRadius: 'var(--radius-sm)',
              maxWidth: '480px',
              width: '100%',
              padding: '24px',
              border: '1px solid var(--line)',
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <h2 className="font-serif" style={{ margin: 0, fontSize: '18px', fontWeight: 500 }}>
                Escalate Ticket #{escalatingTicket.ticketNumber} to Owner
              </h2>
              <button
                onClick={() => setEscalatingTicket(null)}
                style={{ background: 'none', border: 'none', cursor: 'pointer', fontSize: '18px' }}
              >
                &times;
              </button>
            </div>

            <form onSubmit={handleEscalateSubmit}>
              <div style={{ marginBottom: '20px' }}>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, marginBottom: '4px' }}>
                  Escalation Reason *
                </label>
                <textarea
                  rows={3}
                  required
                  placeholder="Explain why Owner intervention is required (e.g. VIP client request, custom weaving dispute, financial adjustment)..."
                  value={escalationReason}
                  onChange={(e) => setEscalationReason(e.target.value)}
                  style={{
                    width: '100%',
                    padding: '8px 12px',
                    fontSize: '13px',
                    border: '1px solid var(--line)',
                    borderRadius: 'var(--radius-sm)',
                  }}
                />
              </div>

              <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end' }}>
                <Button variant="outline" type="button" onClick={() => setEscalatingTicket(null)}>
                  Cancel
                </Button>
                <Button variant="primary" type="submit" isLoading={isSubmittingEscalation}>
                  Confirm Escalation
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
