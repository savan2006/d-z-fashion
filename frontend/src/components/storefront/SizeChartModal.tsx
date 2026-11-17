'use client';

import React, { useState } from 'react';
import { Dialog } from '@/components/ui/Dialog';
import { Table, TableHeader, TableRow, TableHead, TableBody, TableCell } from '@/components/ui/Table';

export interface SizeChartModalProps {
  isOpen: boolean;
  onClose: () => void;
  categoryName?: string;
}

const MEASUREMENTS_INCHES = [
  { size: 'XS', bust: '32', waist: '26', hip: '36', shoulder: '13.5', length: '44' },
  { size: 'S', bust: '34', waist: '28', hip: '38', shoulder: '14.0', length: '44' },
  { size: 'M', bust: '36', waist: '30', hip: '40', shoulder: '14.5', length: '45' },
  { size: 'L', bust: '38', waist: '32', hip: '42', shoulder: '15.0', length: '45' },
  { size: 'XL', bust: '40', waist: '34', hip: '44', shoulder: '15.5', length: '46' },
  { size: 'XXL', bust: '42', waist: '36', hip: '46', shoulder: '16.0', length: '46' },
];

const MEASUREMENTS_CM = [
  { size: 'XS', bust: '81', waist: '66', hip: '91', shoulder: '34.3', length: '112' },
  { size: 'S', bust: '86', waist: '71', hip: '96', shoulder: '35.5', length: '112' },
  { size: 'M', bust: '91', waist: '76', hip: '102', shoulder: '36.8', length: '114' },
  { size: 'L', bust: '96', waist: '81', hip: '107', shoulder: '38.1', length: '114' },
  { size: 'XL', bust: '102', waist: '86', hip: '112', shoulder: '39.4', length: '117' },
  { size: 'XXL', bust: '107', waist: '91', hip: '117', shoulder: '40.6', length: '117' },
];

export const SizeChartModal: React.FC<SizeChartModalProps> = ({
  isOpen,
  onClose,
  categoryName = 'Garment',
}) => {
  const [unit, setUnit] = useState<'inches' | 'cm'>('inches');
  const rows = unit === 'inches' ? MEASUREMENTS_INCHES : MEASUREMENTS_CM;

  return (
    <Dialog
      isOpen={isOpen}
      onClose={onClose}
      title={`${categoryName} Size Guide`}
      description="Standard body measurements tailored for Surat drape and comfortable fit."
      maxWidth="680px"
    >
      <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
        {/* Unit Selector */}
        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
          <button
            type="button"
            onClick={() => setUnit('inches')}
            className={`btn btn-sm ${unit === 'inches' ? 'btn-primary' : 'btn-ghost'}`}
            style={{ minHeight: 'auto', padding: '4px 12px', fontSize: '12px' }}
          >
            Inches
          </button>
          <button
            type="button"
            onClick={() => setUnit('cm')}
            className={`btn btn-sm ${unit === 'cm' ? 'btn-primary' : 'btn-ghost'}`}
            style={{ minHeight: 'auto', padding: '4px 12px', fontSize: '12px' }}
          >
            Centimeters
          </button>
        </div>

        {/* Measurement Table */}
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Size</TableHead>
              <TableHead>Bust ({unit})</TableHead>
              <TableHead>Waist ({unit})</TableHead>
              <TableHead>Hip ({unit})</TableHead>
              <TableHead>Shoulder ({unit})</TableHead>
              <TableHead>Length ({unit})</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {rows.map((row) => (
              <TableRow key={row.size}>
                <TableCell style={{ fontWeight: 600, color: 'var(--wine)' }}>
                  {row.size}
                </TableCell>
                <TableCell>{row.bust}</TableCell>
                <TableCell>{row.waist}</TableCell>
                <TableCell>{row.hip}</TableCell>
                <TableCell>{row.shoulder}</TableCell>
                <TableCell>{row.length}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>

        {/* Measuring Advice Note */}
        <div
          style={{
            padding: '16px',
            backgroundColor: 'var(--paper-warm)',
            borderRadius: 'var(--radius-sm)',
            border: '1px solid var(--line)',
            fontSize: '12px',
            color: 'var(--ink-muted)',
            lineHeight: 1.6,
          }}
        >
          <strong style={{ color: 'var(--ink)', display: 'block', marginBottom: '4px' }}>
            Measuring Tips:
          </strong>
          <ul style={{ margin: 0, paddingLeft: '16px' }}>
            <li><strong>Bust:</strong> Measure around the fullest part of your chest with relaxed arms.</li>
            <li><strong>Waist:</strong> Measure at your natural waistline, typically 2 inches above your navel.</li>
            <li><strong>Hips:</strong> Stand with feet together and measure around the widest point.</li>
            <li><strong>Fit Note:</strong> If your measurements fall between sizes, we recommend selecting the larger size for a relaxed, graceful drape.</li>
          </ul>
        </div>
      </div>
    </Dialog>
  );
};
