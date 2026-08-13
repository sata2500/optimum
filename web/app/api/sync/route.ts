import { NextResponse } from 'next/server';

// In-memory / Global cache store keyed by user email
const userCloudStore: Record<string, any> = {};

export async function POST(request: Request) {
  try {
    const body = await request.json();
    const email = body.userEmail || body.email;

    if (!email) {
      return NextResponse.json({ error: 'User email is required' }, { status: 400 });
    }

    userCloudStore[email.toLowerCase()] = {
      ...body,
      updatedAt: new Date().toISOString(),
    };

    return NextResponse.json({
      success: true,
      message: `Veriler başarıyla eşitlendi (${email})`,
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

  const userData = userCloudStore[email] || null;
  return NextResponse.json({
    success: true,
    email,
    data: userData,
  });
}
