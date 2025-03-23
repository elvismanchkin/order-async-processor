INSERT INTO customers (
    id,
    external_id,
    tax_id,
    name,
    email,
    phone,
    segment,
    created_at,
    status,
    version,
    deleted
) VALUES
      (
          '11111111-1111-1111-1111-111111111111',
          'EXT-1001',
          '1234567890',
          'Test Customer 1',
          'customer1@example.com',
          '+1234567890',
          'REGULAR',
          NOW() - INTERVAL '1 day',
          'ACTIVE',
          0,
          FALSE
      ),
      (
          '22222222-2222-2222-2222-222222222222',
          'EXT-1002',
          '0987654321',
          'Test Customer 2',
          'customer2@example.com',
          '+9876543210',
          'VIP',
          NOW() - INTERVAL '2 days',
          'ACTIVE',
          0,
          FALSE
      ),
      (
          '33333333-3333-3333-3333-333333333333',
          'EXT-1003',
          '5432167890',
          'Test Customer 3',
          'customer3@example.com',
          '+5432167890',
          'REGULAR',
          NOW() - INTERVAL '3 days',
          'INACTIVE',
          0,
          FALSE
      );

-- Insert test data for orders
INSERT INTO orders (
    id,
    reference_number,
    type,
    status,
    customer_id,
    created_by,
    created_at,
    priority,
    due_date,
    description,
    version,
    deleted
) VALUES
      (
          '11111111-2222-3333-4444-555555555555',
          'ORD-001',
          'STANDARD',
          'PENDING',
          '11111111-1111-1111-1111-111111111111',
          'test-user',
          NOW() - INTERVAL '3 hours',
          0,
          NOW() + INTERVAL '2 days',
          'Test order 1',
          0,
          FALSE
      ),
      (
          '22222222-3333-4444-5555-666666666666',
          'ORD-002',
          'PRIORITY',
          'PENDING',
          '22222222-2222-2222-2222-222222222222',
          'test-user',
          NOW() - INTERVAL '2 hours',
          10,
          NOW() + INTERVAL '1 day',
          'Test order 2',
          0,
          FALSE
      ),
      (
          '33333333-4444-5555-6666-777777777777',
          'ORD-003',
          'URGENT',
          'PROCESSING',
          '22222222-2222-2222-2222-222222222222',
          'test-user',
          NOW() - INTERVAL '1 hour',
          20,
          NOW(),
          'Test order 3',
          0,
          FALSE
      ),
      (
          '44444444-5555-6666-7777-888888888888',
          'ORD-004',
          'STANDARD',
          'COMPLETED',
          '11111111-1111-1111-1111-111111111111',
          'test-user',
          NOW() - INTERVAL '5 hours',
          0,
          NOW() - INTERVAL '1 day',
          'Test order 4',
          0,
          FALSE
      );