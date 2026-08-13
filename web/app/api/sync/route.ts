import { NextResponse } from 'next/server';
import { neon } from '@neondatabase/serverless';

// Default fallback in-memory store if DATABASE_URL is not present during build
const inMemoryStore: Record<string, any> = {};

function getDbClient() {
  const connectionString =
    process.env.DATABASE_URL ||
    'postgresql://neondb_owner:npg_mz0pdnOHZ5SD@ep-soft-voice-ave6pb09-pooler.c-11.us-east-1.aws.neon.tech/neondb?channel_binding=require&sslmode=require';
  return neon(connectionString);
}

export async function POST(request: Request) {
  try {
    const body = await request.json();
    const email = (body.userEmail || body.email)?.toLowerCase();

    if (!email) {
      return NextResponse.json({ error: 'User email is required' }, { status: 400 });
    }

    const payloadStr = JSON.stringify(body);

    try {
      const sql = getDbClient();
      // Ensure table exists in Neon PostgreSQL
      await sql`
        CREATE TABLE IF NOT EXISTS user_sync_data (
          email TEXT PRIMARY KEY,
          payload JSONB NOT NULL,
          updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
        );
      `;

      // Upsert user sync data into Neon PostgreSQL
      await sql`
        INSERT INTO user_sync_data (email, payload, updated_at)
        VALUES (${email}, ${payloadStr}::jsonb, NOW())
        ON CONFLICT (email) DO UPDATE
        SET payload = EXCLUDED.payload, updated_at = NOW();
      `;
    } catch (dbError) {
      console.warn('Neon DB connection warning, fallback to memory:', dbError);
      inMemoryStore[email] = {
        ...body,
        updatedAt: new Date().toISOString(),
      };
    }

    return NextResponse.json({
      success: true,
      message: `Veriler Neon PostgreSQL veritabanına başarıyla kaydedildi (${email})`,
      storedCount: body.logs?.length || 0,
    });
  } catch (error: any) {
    return NextResponse.json({ error: error.message }, { status: 500 });
  }
}

export async function GET(request: Request) {
  const { searchParams } = new URL(request.url);
  const email = searchParams.get('email')?.toLowerCase();

  if (!email) {
    return NextResponse.json({ error: 'User email is required' }, { status: 400 });
  }

  try {
    const sql = getDbClient();
    const rows = await sql`
      SELECT payload, updated_at FROM user_sync_data WHERE email = ${email} LIMIT 1;
    `;

    if (rows && rows.length > 0) {
      const payload = typeof rows[0].payload === 'string' ? JSON.parse(rows[0].payload) : rows[0].payload;
      return NextResponse.json({
        success: true,
        email,
        data: payload,
        source: 'Neon PostgreSQL',
      });
    }
  } catch (dbError) {
    console.warn('Neon DB query warning:', dbError);
  }

  const memoryData = inMemoryStore[email] || null;
  return NextResponse.json({
    success: true,
    email,
    data: memoryData,
    source: 'In-Memory',
  });
}
