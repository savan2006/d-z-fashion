import React from 'react';

export const Table: React.FC<React.TableHTMLAttributes<HTMLTableElement>> = ({
  className = '',
  children,
  ...rest
}) => {
  return (
    <div className="table-container">
      <table className={`ui-table ${className}`} {...rest}>
        {children}
      </table>
    </div>
  );
};

export const TableHeader: React.FC<React.HTMLAttributes<HTMLTableSectionElement>> = ({
  className = '',
  children,
  ...rest
}) => <thead className={className} {...rest}>{children}</thead>;

export const TableBody: React.FC<React.HTMLAttributes<HTMLTableSectionElement>> = ({
  className = '',
  children,
  ...rest
}) => <tbody className={className} {...rest}>{children}</tbody>;

export const TableFooter: React.FC<React.HTMLAttributes<HTMLTableSectionElement>> = ({
  className = '',
  children,
  ...rest
}) => <tfoot className={className} {...rest}>{children}</tfoot>;

export const TableRow: React.FC<React.HTMLAttributes<HTMLTableRowElement>> = ({
  className = '',
  children,
  ...rest
}) => <tr className={className} {...rest}>{children}</tr>;

export const TableHead: React.FC<React.ThHTMLAttributes<HTMLTableCellElement>> = ({
  className = '',
  children,
  ...rest
}) => <th className={className} {...rest}>{children}</th>;

export const TableCell: React.FC<React.TdHTMLAttributes<HTMLTableCellElement>> = ({
  className = '',
  children,
  ...rest
}) => <td className={className} {...rest}>{children}</td>;
