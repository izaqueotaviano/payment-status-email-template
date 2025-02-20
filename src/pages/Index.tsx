
import { CheckCircle } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";

const Index = () => {
  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center p-4">
      <Card className="w-full max-w-[600px] p-8 space-y-6 animate-fade-in">
        {/* Header */}
        <div className="text-center space-y-4">
          <div className="inline-flex items-center justify-center w-16 h-16 bg-emerald-100 rounded-full animate-success-bounce">
            <CheckCircle className="w-8 h-8 text-emerald-600" />
          </div>
          <div>
            <h1 className="text-2xl font-semibold text-gray-900">Pagamento Aprovado</h1>
            <p className="text-gray-600 mt-2">Seu pagamento foi processado com sucesso</p>
          </div>
        </div>

        <Separator />

        {/* Transaction Details */}
        <div className="space-y-4">
          <h2 className="text-lg font-medium text-gray-900">Detalhes da Transação</h2>
          <div className="grid grid-cols-2 gap-4 text-sm">
            <div>
              <p className="text-gray-500">ID do Pedido</p>
              <p className="font-medium text-gray-900">#123456789</p>
            </div>
            <div>
              <p className="text-gray-500">Data</p>
              <p className="font-medium text-gray-900">21/03/2024</p>
            </div>
            <div>
              <p className="text-gray-500">Método</p>
              <p className="font-medium text-gray-900">Cartão de Crédito</p>
            </div>
            <div>
              <p className="text-gray-500">Valor</p>
              <p className="font-medium text-gray-900">R$ 299,90</p>
            </div>
          </div>
        </div>

        <Separator />

        {/* Actions */}
        <div className="space-y-4">
          <Button className="w-full bg-emerald-600 hover:bg-emerald-700 transition-colors">
            Ver Detalhes do Pedido
          </Button>
        </div>

        {/* Footer */}
        <div className="text-center text-sm text-gray-500 space-y-2">
          <p>Precisa de ajuda? Entre em contato com nosso suporte</p>
          <p>suporte@empresa.com.br | (11) 99999-9999</p>
        </div>
      </Card>
    </div>
  );
};

export default Index;
