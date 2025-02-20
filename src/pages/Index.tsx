
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

        {/* Order Status Table */}
        <div className="space-y-4">
          {/* Order Number and Track Button */}
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <h2 className="text-lg font-medium text-gray-900">Status do Pedido</h2>
              <span className="text-sm text-gray-500">#{123456789}</span>
            </div>
            <Button 
              variant="outline" 
              className="border-emerald-600 text-emerald-600 hover:bg-emerald-50"
            >
              Rastrear Pedido
            </Button>
          </div>
          
          <table className="w-full border-collapse">
            <tbody>
              {/* Order Placed */}
              <tr>
                <td className="py-4 w-12">
                  <div className="flex-shrink-0 w-11 h-11 rounded-full bg-emerald-600 flex items-center justify-center">
                    <CheckCircle className="w-5 h-5 text-white" />
                  </div>
                </td>
                <td className="py-4 pl-4">
                  <h3 className="font-medium text-gray-900">Pedido Realizado</h3>
                  <p className="text-sm text-gray-500">21/03/2024 às 10:30</p>
                </td>
              </tr>
              {/* Payment Approved */}
              <tr>
                <td className="py-4 w-12">
                  <div className="flex-shrink-0 w-11 h-11 rounded-full bg-emerald-600 flex items-center justify-center">
                    <CheckCircle className="w-5 h-5 text-white" />
                  </div>
                </td>
                <td className="py-4 pl-4">
                  <h3 className="font-medium text-gray-900">Pagamento Aprovado</h3>
                  <p className="text-sm text-gray-500">21/03/2024 às 10:35</p>
                </td>
              </tr>
              {/* Order Shipped */}
              <tr>
                <td className="py-4 w-12">
                  <div className="flex-shrink-0 w-11 h-11 rounded-full bg-gray-200 flex items-center justify-center">
                    <CheckCircle className="w-5 h-5 text-gray-400" />
                  </div>
                </td>
                <td className="py-4 pl-4">
                  <h3 className="font-medium text-gray-400">Pedido Enviado</h3>
                  <p className="text-sm text-gray-400">Aguardando</p>
                </td>
              </tr>
              {/* Order Delivered */}
              <tr>
                <td className="py-4 w-12">
                  <div className="flex-shrink-0 w-11 h-11 rounded-full bg-gray-200 flex items-center justify-center">
                    <CheckCircle className="w-5 h-5 text-gray-400" />
                  </div>
                </td>
                <td className="py-4 pl-4">
                  <h3 className="font-medium text-gray-400">Pedido Entregue</h3>
                  <p className="text-sm text-gray-400">Aguardando</p>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <Separator />

        {/* Transaction Details */}
        <div className="space-y-4">
          <h2 className="text-lg font-medium text-gray-900">Detalhes da Transação</h2>
          <table className="w-full">
            <tbody>
              <tr>
                <td className="py-2 w-1/2">
                  <p className="text-gray-500">ID do Pedido</p>
                  <p className="font-medium text-gray-900">#123456789</p>
                </td>
                <td className="py-2 w-1/2">
                  <p className="text-gray-500">Data</p>
                  <p className="font-medium text-gray-900">21/03/2024</p>
                </td>
              </tr>
              <tr>
                <td className="py-2">
                  <p className="text-gray-500">Método</p>
                  <p className="font-medium text-gray-900">Cartão de Crédito</p>
                </td>
                <td className="py-2">
                  <p className="text-gray-500">Valor</p>
                  <p className="font-medium text-gray-900">R$ 299,90</p>
                </td>
              </tr>
            </tbody>
          </table>
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
