'use client';

import React, { useEffect, useState } from 'react';
import { adminApi, ManagerStaffResponse } from '@/lib/api/admin';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Dialog } from '@/components/ui/Dialog';
import { TextField } from '@/components/ui/TextField';
import { Skeleton } from '@/components/ui/Skeleton';
import { useToast } from '@/components/ui/Toast';
import { Plus, Key } from 'lucide-react';

export default function AdminAccountsPage() {
  const { toast } = useToast();
  const [loading, setLoading] = useState(true);
  const [managers, setManagers] = useState<ManagerStaffResponse[]>([]);

  // Create Manager Modal
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [fullName, setFullName] = useState('');
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [creating, setCreating] = useState(false);

  // Reset Password Modal
  const [resetTarget, setResetTarget] = useState<ManagerStaffResponse | null>(null);
  const [newPassword, setNewPassword] = useState('');
  const [resetting, setResetting] = useState(false);

  const loadManagers = () => {
    let isMounted = true;
    adminApi.listManagers()
      .then((res) => {
        if (!isMounted || !res.data) return;
        setManagers(res.data);
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
    loadManagers();
  }, []);

  const handleCreateManager = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!fullName.trim() || !username.trim() || !email.trim() || password.length < 8) {
      toast({ type: 'warning', title: 'Invalid Form', message: 'All fields required; password must be at least 8 characters.' });
      return;
    }

    setCreating(true);
    try {
      await adminApi.createManager({
        fullName: fullName.trim(),
        username: username.trim(),
        email: email.trim(),
        password,
      });

      toast({
        type: 'success',
        title: 'Manager Account Created',
        message: `Account for ${fullName} created. Credentials ready for warehouse access.`,
      });
      setCreateModalOpen(false);
      setFullName('');
      setUsername('');
      setEmail('');
      setPassword('');
      loadManagers();
    } catch (err: unknown) {
      const msg = (err as { message?: string })?.message || 'Creation failed';
      toast({ type: 'error', title: 'Error', message: msg });
    } finally {
      setCreating(false);
    }
  };

  const handleResetPassword = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!resetTarget || newPassword.length < 8) {
      toast({ type: 'warning', title: 'Weak Password', message: 'Password must be at least 8 characters.' });
      return;
    }

    setResetting(true);
    try {
      await adminApi.resetManagerPassword(resetTarget.id, newPassword);
      toast({
        type: 'success',
        title: 'Password Reset',
        message: `Password for @${resetTarget.username} has been updated.`,
      });
      setResetTarget(null);
      setNewPassword('');
    } catch (err: unknown) {
      const msg = (err as { message?: string })?.message || 'Password reset failed';
      toast({ type: 'error', title: 'Error', message: msg });
    } finally {
      setResetting(false);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <h1 className="font-serif" style={{ margin: 0, fontSize: '22px', fontWeight: 500 }}>
            Staff &amp; Manager Accounts
          </h1>
          <p style={{ margin: '4px 0 0', fontSize: '13px', color: 'var(--ink-muted)' }}>
            Provision Surat warehouse operations staff accounts and reset operational access credentials.
          </p>
        </div>

        <Button variant="primary" onClick={() => setCreateModalOpen(true)}>
          <Plus size={16} style={{ marginRight: '6px' }} />
          Provision Manager
        </Button>
      </div>

      {/* Staff Table */}
      <div
        style={{
          backgroundColor: '#ffffff',
          border: '1px solid var(--line)',
          borderRadius: 'var(--radius-sm)',
          overflow: 'hidden',
        }}
      >
        <div style={{ padding: '16px 20px', borderBottom: '1px solid var(--line-light)' }}>
          <h2 style={{ margin: 0, fontSize: '15px', fontWeight: 600 }}>Active Warehouse Managers ({managers.length})</h2>
        </div>

        {loading ? (
          <div style={{ padding: '24px' }}>
            <Skeleton height="60px" style={{ marginBottom: '12px' }} />
            <Skeleton height="60px" />
          </div>
        ) : managers.length === 0 ? (
          <div style={{ padding: '48px 24px', textAlign: 'center', color: 'var(--ink-muted)', fontSize: '13px' }}>
            No managers provisioned.
          </div>
        ) : (
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '13px' }}>
            <thead>
              <tr style={{ backgroundColor: 'var(--surface-subtle)', borderBottom: '1px solid var(--line)' }}>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Staff Name</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Username</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Email Address</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Role</th>
                <th style={{ padding: '12px 16px', fontWeight: 600 }}>Status</th>
                <th style={{ padding: '12px 16px', fontWeight: 600, textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {managers.map((m) => (
                <tr key={m.id} style={{ borderBottom: '1px solid var(--line-light)' }}>
                  <td style={{ padding: '14px 16px', fontWeight: 600, color: 'var(--ink)' }}>
                    {m.fullName}
                  </td>
                  <td style={{ padding: '14px 16px', color: 'var(--ink-muted)' }}>
                    @{m.username}
                  </td>
                  <td style={{ padding: '14px 16px', color: 'var(--ink)' }}>
                    {m.email}
                  </td>
                  <td style={{ padding: '14px 16px' }}>
                    <Badge variant="neutral">MANAGER</Badge>
                  </td>
                  <td style={{ padding: '14px 16px' }}>
                    <Badge variant={m.active ? 'success' : 'neutral'}>
                      {m.active ? 'Active' : 'Suspended'}
                    </Badge>
                  </td>
                  <td style={{ padding: '14px 16px', textAlign: 'right' }}>
                    <Button variant="outline" size="sm" onClick={() => setResetTarget(m)}>
                      <Key size={12} style={{ marginRight: '4px' }} />
                      Reset Password
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* Provision Manager Modal */}
      {createModalOpen && (
        <Dialog
          isOpen={true}
          onClose={() => setCreateModalOpen(false)}
          title="Provision New Manager Account"
          description="Create operational credentials for Surat warehouse supervisor."
        >
          <form onSubmit={handleCreateManager} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <TextField
              label="Full Name"
              placeholder="e.g. Ramesh Shah"
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
              required
            />

            <TextField
              label="Username"
              placeholder="e.g. rameshshah"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
            />

            <TextField
              label="Email Address"
              type="email"
              placeholder="ramesh@dzfashion.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />

            <TextField
              label="Initial Password"
              type="password"
              placeholder="At least 8 characters"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '8px' }}>
              <Button type="button" variant="outline" onClick={() => setCreateModalOpen(false)} disabled={creating}>
                Cancel
              </Button>
              <Button type="submit" variant="primary" isLoading={creating}>
                Create Account
              </Button>
            </div>
          </form>
        </Dialog>
      )}

      {/* Reset Password Modal */}
      {resetTarget && (
        <Dialog
          isOpen={true}
          onClose={() => setResetTarget(null)}
          title={`Reset Password: ${resetTarget.fullName}`}
          description={`Provision a new secure password for @${resetTarget.username}.`}
        >
          <form onSubmit={handleResetPassword} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <TextField
              label="New Password"
              type="password"
              placeholder="At least 8 characters"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              required
            />

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '8px' }}>
              <Button type="button" variant="outline" onClick={() => setResetTarget(null)} disabled={resetting}>
                Cancel
              </Button>
              <Button type="submit" variant="primary" isLoading={resetting}>
                Update Password
              </Button>
            </div>
          </form>
        </Dialog>
      )}
    </div>
  );
}
