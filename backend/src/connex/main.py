from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from connex.settings import settings
from connex.api.routes import auth, rooms, users, keys, uploads, ws
from connex.db.init import init_db

app = FastAPI(
    title=settings.PROJECT_NAME,
    openapi_url=f"{settings.API_V1_STR}/openapi.json"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.on_event("startup")
async def startup() -> None:
    await init_db()

@app.get("/")
def root():
    return {"message": "ConneX Backend is running"}

app.include_router(auth.router, prefix=f"{settings.API_V1_STR}/auth", tags=["auth"])
app.include_router(users.router, prefix=f"{settings.API_V1_STR}/users", tags=["users"])
app.include_router(rooms.router, prefix=f"{settings.API_V1_STR}/rooms", tags=["rooms"])
app.include_router(keys.router, prefix=f"{settings.API_V1_STR}/keys", tags=["keys"])
app.include_router(uploads.router, prefix=f"{settings.API_V1_STR}/uploads", tags=["uploads"])
app.include_router(ws.router, prefix="/ws", tags=["ws"])
